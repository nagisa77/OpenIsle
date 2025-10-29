import { Agent, Runner, hostedMcpTool, withTrace, webSearchTool } from "@openai/agents";

export type WorkflowInput = { input_as_text: string };

export abstract class BotFather {
  protected readonly openisleToken = (process.env.OPENISLE_TOKEN ?? "").trim();
  protected readonly weatherToken = (process.env.APIFY_API_TOKEN ?? "").trim();

  protected readonly openisleMcp = this.createHostedMcpTool();
  protected readonly weatherMcp = this.createWeatherMcpTool();
  protected readonly webSearchPreview = this.createWebSearchPreviewTool();
  protected readonly agent: Agent;

  constructor(protected readonly name: string) {
    console.log(`✅ ${this.name} starting...`);
    console.log(
      this.openisleToken
        ? "🔑 OPENISLE_TOKEN detected in environment; it will be attached to MCP requests."
        : "🔓 OPENISLE_TOKEN not set; authenticated MCP tools may be unavailable."
    );

    console.log(
      this.weatherToken
        ? "☁️ APIFY_API_TOKEN detected; weather MCP server will be available."
        : "🌥️ APIFY_API_TOKEN not set; weather updates will be unavailable."
    );

    this.agent = new Agent({
      name: this.name,
      instructions: this.buildInstructions(),
      tools: [
        this.openisleMcp, 
        this.weatherMcp, 
        this.webSearchPreview
      ],
      model: "gpt-4o",
      modelSettings: {
        temperature: 0.7,
        topP: 1,
        maxTokens: 2048,
        toolChoice: "auto",
        store: true,
      },
    });
  }

  protected buildInstructions(): string {
    const instructions = [
      ...this.getBaseInstructions(),
      ...this.getAdditionalInstructions(),
    ].filter(Boolean);
    return instructions.join("\n");
  }

  protected getBaseInstructions(): string[] {
    return [
      "You are a helpful assistant for https://www.open-isle.com.",
      "Finish tasks end-to-end before replying. If multiple MCP tools are needed, call them sequentially until the task is truly done.",
      "When presenting the result, reply in Chinese with a concise summary and include any important URLs or IDs.",
      "After finishing replies, call mark_notifications_read with all processed notification IDs to keep the inbox clean.",
    ];
  }

  private createWebSearchPreviewTool() {
    return webSearchTool({
      userLocation: {
        type: "approximate",
        country: undefined,
        region: undefined,
        city: undefined,
        timezone: undefined
      },
      searchContextSize: "medium"
    })
  }

  private createHostedMcpTool() {
    const token = this.openisleToken;
    const authConfig = token
      ? {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      : {};

    return hostedMcpTool({
      serverLabel: "openisle_mcp",
      serverUrl: "https://www.open-isle.com/mcp",
      allowedTools: [
        "search", // 用于搜索帖子、内容等
        "create_post", // 创建新帖子
        "reply_to_post", // 回复帖子
        "reply_to_comment", // 回复评论
        "recent_posts", // 获取最新帖子
        "get_post", // 获取特定帖子的详细信息
        "list_unread_messages", // 列出未读消息或通知
        "mark_notifications_read", // 标记通知为已读
      ],
      requireApproval: "never",
      ...authConfig,
    });
  }

  private createWeatherMcpTool(): ReturnType<typeof hostedMcpTool> {
    return hostedMcpTool({
      serverLabel: "weather_mcp_server",
      serverUrl: "https://jiri-spilka--weather-mcp-server.apify.actor/mcp",
      requireApproval: "never",
      allowedTools: [
        "get_current_weather", // 天气 MCP 工具
      ],
      headers: {
        Authorization: `Bearer ${this.weatherToken || ""}`,
      },
    });
  }

  protected getAdditionalInstructions(): string[] {
    return [];
  }

  protected createRunner(): Runner {
    return new Runner({
      workflowName: this.name,
      traceMetadata: {
        __trace_source__: "agent-builder",
        workflow_id: "wf_69003cbd47e08190928745d3c806c0b50d1a01cfae052be8",
      },
    });
  }

  public async runWorkflow(workflow: WorkflowInput) {
    if (!process.env.OPENAI_API_KEY) {
      throw new Error("Missing OPENAI_API_KEY");
    }

    const runner = this.createRunner();

    return await withTrace(`${this.name} run`, async () => {
      const preview = workflow.input_as_text.trim();
      console.log(
        "📝 Received workflow input (preview):",
        preview.length > 200 ? `${preview.slice(0, 200)}…` : preview
      );

      console.log("🚦 Starting agent run with maxTurns=16...");
      const result = await runner.run(this.agent, workflow.input_as_text, {
        maxTurns: 16,
      });

      console.log("📬 Agent run completed. Result keys:", Object.keys(result));

      if (!result.finalOutput) {
        throw new Error("Agent result is undefined (no final output).");
      }

      const openisleBotResult = { output_text: String(result.finalOutput) };

      console.log(
        "🤖 Agent result (length=%d):\n%s",
        openisleBotResult.output_text.length,
        openisleBotResult.output_text
      );
      return openisleBotResult;
    });
  }

  protected abstract getCliQuery(): string;

  public async runCli(): Promise<void> {
    try {
      const query = this.getCliQuery();
      console.log("🔍 Running workflow...");
      await this.runWorkflow({ input_as_text: query });
      process.exit(0);
    } catch (err: any) {
      console.error("❌ Agent failed:", err?.stack || err);
      process.exit(1);
    }
  }
}
