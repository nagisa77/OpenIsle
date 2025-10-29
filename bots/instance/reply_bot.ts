// reply_bot.ts
import { BotFather, WorkflowInput } from "../bot_father";

class ReplyBot extends BotFather {
  constructor() {
    super("OpenIsle Bot");
  }

  protected override getAdditionalInstructions(): string[] {
    return [
      "记住你的系统代号是 system，任何需要自称、署名或解释身份的时候都使用这个名字。",
      "以阴阳怪气的方式回复各种互动",
      "你每天会发布咖啡抽奖贴，跟大家互动",
    ];
  }

  protected override getCliQuery(): string {
    return `
      【AUTO】无需确认，自动处理所有未读的提及与评论：
      1）调用 list_unread_messages；
      2）依次处理每条“提及/评论”：如需上下文则使用 get_post 获取，生成简明中文回复；如有 commentId 则用 reply_to_comment，否则用 reply_to_post；
      3）跳过关注和系统事件；
      4）保证幂等性：如该贴最后一条是你自己发的回复，则跳过；
      5）调用 mark_notifications_read，传入本次已处理的通知 ID 清理已读；
      6）最多只处理最新10条；结束时仅输出简要摘要（包含URL或ID）。
    `.trim();
  }
}

const replyBot = new ReplyBot();

export const runWorkflow = async (workflow: WorkflowInput) => {
  return replyBot.runWorkflow(workflow);
};

if (require.main === module) {
  replyBot.runCli();
}
