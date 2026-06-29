package com.aizuda.snail.ai.agent.core.tool;

import cn.hutool.core.util.StrUtil;
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

/**
 * Shell 命令执行工具，在指定工作目录执行命令并采集输出（支持超时强杀与输出行数截断），供 Agent 作为工具调用。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Slf4j
public class ShellTool {

    /**
     * 命令执行的工作目录
     */
    private final String workingDirectory;
    /**
     * 命令超时时间（毫秒），超时则强制终止进程
     */
    private final long commandTimeout;
    /**
     * 采集输出的最大行数，超出部分截断
     */
    private final int maxOutputLines;

    public ShellTool(String workingDirectory, long commandTimeout, int maxOutputLines) {
        this.workingDirectory = workingDirectory;
        this.commandTimeout = commandTimeout;
        this.maxOutputLines = maxOutputLines;
    }

    /**
     * 执行 Shell 命令并返回采集到的输出（含截断提示与非零退出码）
     *
     * @param command 待执行的 Shell 命令
     * @return 命令输出文本，或以 "Error:" 开头的错误说明
     */
    @Tool(name = "shell", description = "Execute a shell command. Can be used to run scripts in the skill directory, view files, install dependencies, etc. "
            + "Supports chained commands (&& or ;).")
    public String execute(@ToolParam(description = "The shell command to execute") String command) {
        log.info("shell:{}", command);

        if (StrUtil.isBlank(command)) {
            return "Error: command cannot be empty";
        }

        try {
            Path workPath = Paths.get(workingDirectory);
            if (!Files.exists(workPath) || !Files.isDirectory(workPath)) {
                return "Error: 工作目录不存在: " + workingDirectory;
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
            Thread readerThread = Thread.ofVirtual()
                    .name("shell-output-reader")
                    .start(() -> readOutput(process, output, stats));

            // 等待进程结束，超时则杀死
            boolean finished = process.waitFor(commandTimeout, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                readerThread.interrupt();
                return "Error: 命令执行超时 (" + (commandTimeout / 1000) + "秒)";
            }

            readerThread.join(TimeUnit.SECONDS.toMillis(5));
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

    /**
     * 输出采集统计：累计行数与是否已截断
     */
    private static class OutputStats {
        private int lineCount;
        private boolean truncated;
    }
}
