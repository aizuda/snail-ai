package com.aizuda.snail.ai.agent.core.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ShellTool {

    private final String workingDirectory;
    private final long commandTimeout;
    private final int maxOutputLines;

    public ShellTool(String workingDirectory, long commandTimeout, int maxOutputLines) {
        this.workingDirectory = workingDirectory;
        this.commandTimeout = commandTimeout;
        this.maxOutputLines = maxOutputLines;
    }

    @Tool(name = "shell", description = "Execute a shell command. Can be used to run scripts in the skill directory, view files, install dependencies, etc. "
            + "Ensure the working directory is correct before use. Supports chained commands (&& or ;).")
    public String execute(
            @ToolParam(description = "The shell command to execute") String command,
            @ToolParam(description = "Working directory (optional, defaults to skill directory)", required = false) String workDir) {
        log.info("shell command requested");

        if (command == null || command.trim().isEmpty()) {
            return "Error: command cannot be empty";
        }

        try {
            Path basePath = Paths.get(workingDirectory).toAbsolutePath().normalize();
            Path workPath = resolveWorkPath(basePath, workDir);
            if (!Files.exists(workPath) || !Files.isDirectory(workPath)) {
                return "Error: 工作目录不存在: " + workPath;
            }

            // 检测操作系统，构建命令
            String[] shellCommand = getShellCommand(command);

            ProcessBuilder pb = new ProcessBuilder(shellCommand);
            pb.directory(workPath.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // 读取输出
            StringBuilder output = new StringBuilder();
            OutputStats stats = new OutputStats();
            ExecutorService readerExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "shell-output-reader");
                thread.setDaemon(true);
                return thread;
            });
            Future<?> readerFuture = readerExecutor.submit(() -> readOutput(process, output, stats));

            // 等待进程结束，超时则杀死
            boolean finished = process.waitFor(commandTimeout, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                readerFuture.cancel(true);
                readerExecutor.shutdownNow();
                return "Error: 命令执行超时 (" + (commandTimeout / 1000) + "秒)";
            }

            readerFuture.get(5, TimeUnit.SECONDS);
            readerExecutor.shutdownNow();
            int exitCode = process.exitValue();

            // 构建结果
            StringBuilder result = new StringBuilder();
            if (output.length() == 0) {
                result.append("<无输出>");
            } else {
                result.append(output);
            }

            if (stats.truncated) {
                result.append("\n\n... 输出已截断，显示前 ")
                        .append(maxOutputLines).append(" 行 (共 ")
                        .append(stats.lineCount).append(" 行)");
            }

            if (exitCode != 0) {
                result.append("\n\nExit code: ").append(exitCode);
            }

            return result.toString();

        } catch (Exception e) {
            log.error("Shell 命令执行失败: {}", command, e);
            return "Error: " + e.getMessage();
        }
    }

    private Path resolveWorkPath(Path basePath, String workDir) {
        Path requestedPath = (workDir != null && !workDir.trim().isEmpty())
                ? Paths.get(workDir.trim())
                : basePath;
        Path resolved = requestedPath.isAbsolute()
                ? requestedPath.toAbsolutePath().normalize()
                : basePath.resolve(requestedPath).normalize();
        if (!resolved.startsWith(basePath)) {
            throw new IllegalArgumentException("工作目录必须位于技能目录内");
        }
        return resolved;
    }

    private void readOutput(Process process, StringBuilder output, OutputStats stats) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stats.lineCount++;
                if (stats.lineCount <= maxOutputLines) {
                    if (output.length() > 0) {
                        output.append("\n");
                    }
                    output.append(line);
                } else {
                    stats.truncated = true;
                }
            }
        } catch (Exception e) {
            log.debug("Failed to read shell output: {}", e.getMessage());
        }
    }

    private String[] getShellCommand(String command) {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return new String[]{"cmd", "/c", command};
        } else {
            return new String[]{"/bin/sh", "-c", command};
        }
    }

    private static class OutputStats {
        private int lineCount;
        private boolean truncated;
    }
}
