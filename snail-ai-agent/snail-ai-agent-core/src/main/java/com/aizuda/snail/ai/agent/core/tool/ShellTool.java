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
        log.info("shell:{}", command);

        if (command == null || command.trim().isEmpty()) {
            return "Error: command cannot be empty";
        }

        try {
            // 确定工作目录
            String effectiveWorkDir = (workDir != null && !workDir.trim().isEmpty())
                    ? workDir : workingDirectory;
            Path workPath = Paths.get(effectiveWorkDir);
            if (!Files.exists(workPath) || !Files.isDirectory(workPath)) {
                return "Error: 工作目录不存在: " + effectiveWorkDir;
            }

            // 检测操作系统，构建命令
            String[] shellCommand = getShellCommand(command);

            ProcessBuilder pb = new ProcessBuilder(shellCommand);
            pb.directory(workPath.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // 读取输出
            StringBuilder output = new StringBuilder();
            int lineCount = 0;
            boolean truncated = false;

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lineCount++;
                    if (lineCount <= maxOutputLines) {
                        if (output.length() > 0) {
                            output.append("\n");
                        }
                        output.append(line);
                    } else {
                        truncated = true;
                    }
                }
            }

            // 等待进程结束，超时则杀死
            boolean finished = process.waitFor(commandTimeout, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "Error: 命令执行超时 (" + (commandTimeout / 1000) + "秒)";
            }

            int exitCode = process.exitValue();

            // 构建结果
            StringBuilder result = new StringBuilder();
            if (output.length() == 0) {
                result.append("<无输出>");
            } else {
                result.append(output);
            }

            if (truncated) {
                result.append("\n\n... 输出已截断，显示前 ")
                        .append(maxOutputLines).append(" 行 (共 ")
                        .append(lineCount).append(" 行)");
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

    private String[] getShellCommand(String command) {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return new String[]{"cmd", "/c", command};
        } else {
            return new String[]{"/bin/sh", "-c", command};
        }
    }
}
