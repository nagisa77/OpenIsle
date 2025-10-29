import { BotFather, WorkflowInput } from "../bot_father";

const WEEKDAY_NAMES = ["日", "一", "二", "三", "四", "五", "六"] as const;

class DailyNewsBot extends BotFather {
  constructor() {
    super("Daily News Bot");
  }

  protected override getAdditionalInstructions(): string[] {
    return [
      "You are DailyNewsBot，专职在 OpenIsle 发布每日新闻速递。",
      "始终使用简体中文回复，并以结构化 Markdown 呈现内容。",
      "发布内容前务必完成资讯核实：分别通过 web_search 调研 CoinDesk 今日所有要闻、Reuters 今日重点新闻，以及全球 AI 领域的重大进展。",
      "整合新闻时，将同源资讯合并，突出影响力、涉及主体与潜在影响，保持语句简洁。",
      "所有新闻要点都要附带来源链接，并在括号中标注来源站点名。",
      "使用 weather_mcp_server 的 get_current_weather 获取北京、上海、广州、深圳的天气，并在正文中列表展示",
      "正文结尾补充一个行动建议或提醒，帮助读者快速把握重点。",
      "严禁发布超过一篇帖子，create_post 只调用一次。",
    ];
  }

  protected override getCliQuery(): string {
    const now = new Date(Date.now() + 8 * 60 * 60 * 1000);
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const day = String(now.getDate()).padStart(2, "0");
    const weekday = WEEKDAY_NAMES[now.getDay()];
    const dateLabel = `${year}年${month}月${day}日 星期${weekday}`;
    const isoDate = `${year}-${month}-${day}`;
    const categoryId = Number(process.env.DAILY_NEWS_CATEGORY_ID ?? "6");
    const tagIdsEnv = process.env.DAILY_NEWS_TAG_IDS ?? "3,33";
    const tagIds = tagIdsEnv
      .split(",")
      .map((id) => Number(id.trim()))
      .filter((id) => !Number.isNaN(id));
    const finalTagIds = tagIds.length > 0 ? tagIds : [1];
    const tagIdsText = `[${finalTagIds.join(", ")}]`;

    return `
请立即在 https://www.open-isle.com 使用 create_post 发布一篇名为「OpenIsle 每日新闻速递｜${dateLabel}」的帖子，并遵循以下要求：
1. 发布类型为 NORMAL，categoryId = ${categoryId}，tagIds = ${tagIdsText}。
2. 正文以简洁问候开头, 不用再重复标题
3. 使用 web_search 工具按以下顺序收集资讯，并在正文中以 Markdown 小节呈现：
   - 「全球区块链与加密」：汇总 CoinDesk 在 ${isoDate}（UTC+8 当日）发布的所有重点新闻, 列出至少5条
   - 「国际财经速览」：汇总 Reuters 当日重点头条，关注宏观经济、市场波动或政策变化。列出至少5条
   - 「AI 行业快讯」：检索全球 AI 领域的重要发布或事件（例如 OpenAI、Google、Meta、国内大模型厂商等）。列出至少5条
4. 每条新闻采用项目符号，先写结论再给出关键数字或细节，末尾添加来源超链接，格式示例：「**结论** —— 关键细节。（来源：[Reuters](URL)）」
5. 资讯整理完毕后，调用 weather_mcp_server.get_current_weather，列出北京、上海、广州、深圳今日天气，放置在「城市天气」小节下, 本小节可加emoji。
6. 最后一节为「今日提醒」，给出 2-3 条与新闻或天气相关的行动建议。
7. 若在资讯搜集过程中发现相互矛盾的信息，须在正文中以「⚠️ 风险提示」说明原因及尚待确认的点。
9. 发布完成后，不要再次调用 create_post。
`.trim();
  }
}

const dailyNewsBot = new DailyNewsBot();

export const runWorkflow = async (workflow: WorkflowInput) => {
  return dailyNewsBot.runWorkflow(workflow);
};

if (require.main === module) {
  dailyNewsBot.runCli();
}
