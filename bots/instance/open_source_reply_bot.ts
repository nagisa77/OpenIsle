import { readFileSync } from "node:fs";
import path from "node:path";
import { BotFather, WorkflowInput } from "../bot_father";

class OpenSourceReplyBot extends BotFather {
  constructor() {
    super("OpenSource Reply Bot");
  }

  protected override getAdditionalInstructions(): string[] {
    const knowledgeBase = this.loadKnowledgeBase();

    return [
      "You are OpenSourceReplyBot, a professional helper who focuses on answering open-source development and code-related questions for the OpenIsle community.",
      "Respond in Chinese using well-structured Markdown sections such as 标题、列表、代码块等，让回复清晰易读。",
      "保持语气专业、耐心、详尽，绝不使用表情符号或颜文字，也不要卖萌。",
      "优先解答与项目代码、贡献流程、架构设计或排错相关的问题；",
      "在需要时引用 README.md 与 CONTRIBUTING.md 中的要点，帮助用户快速定位文档位置。",
      knowledgeBase,
    ].filter(Boolean);
  }

  protected override getCliQuery(): string {
    return `
【AUTO】每30分钟自动巡检未读提及与评论，严格遵守以下流程：
1）调用 list_unread_messages 获取待处理的“提及/评论”；
2）按时间从新到旧逐条处理（最多10条）；如需上下文请调用 get_post；
3）仅对与开源项目、代码实现或贡献流程直接相关的问题生成详尽的 Markdown 中文回复，
   若与主题无关则礼貌说明并跳过；
4）回复时引用 README 或 CONTRIBUTING 中的要点（如适用），并优先给出可执行的排查步骤或代码建议；
5）回复评论使用 reply_to_comment，回复帖子使用 reply_to_post；
6）若某通知最后一条已由本 bot 回复，则跳过避免重复；
7）整理已处理通知 ID 调用 mark_notifications_read；
8）结束时输出包含处理条目概览（URL或ID）的总结。`.trim();
  }

  private loadKnowledgeBase(): string {
    const docs = ["../../README.md", "../../CONTRIBUTING.md"];
    const sections: string[] = [];

    for (const relativePath of docs) {
      try {
        const absolutePath = path.resolve(__dirname, relativePath);
        const content = readFileSync(absolutePath, "utf-8").trim();
        if (content) {
          sections.push(`以下是 ${path.basename(absolutePath)} 的内容：\n${content}`);
        }
      } catch (error) {
        sections.push(`未能加载 ${relativePath}，请检查文件路径或权限。`);
      }
    }

    return sections.join("\n\n");
  }
}

const openSourceReplyBot = new OpenSourceReplyBot();

export const runWorkflow = async (workflow: WorkflowInput) => {
  return openSourceReplyBot.runWorkflow(workflow);
};

if (require.main === module) {
  openSourceReplyBot.runCli();
}
