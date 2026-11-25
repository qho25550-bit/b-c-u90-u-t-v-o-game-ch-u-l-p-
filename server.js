import express from "express";
import helmet from "helmet";
import cors from "cors";
import rateLimit from "express-rate-limit";
import dotenv from "dotenv";

dotenv.config();

const app = express();
app.use(helmet());
app.use(cors()); // Khi triển khai thật, hãy giới hạn origin của ứng dụng/web quản trị.
app.use(express.json({ limit: "20kb" }));
app.use(rateLimit({ windowMs: 60 * 1000, limit: 8 }));

const PORT = process.env.PORT || 3000;
const OPENAI_MODEL = process.env.OPENAI_MODEL || "gpt-6.2-astra";
const GEMINI_MODEL = process.env.GEMINI_MODEL || "gemini-3.8-flash";

function requireKeys() {
  if (!process.env.OPENAI_API_KEY || !process.env.GEMINI_API_KEY) {
    throw new Error("Máy chủ chưa cấu hình đủ OPENAI_API_KEY và GEMINI_API_KEY.");
  }
}

async function openai(prompt) {
  const response = await fetch("https://api.openai.com/v1/responses", {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${process.env.OPENAI_API_KEY}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      model: OPENAI_MODEL,
      input: prompt
    })
  });
  const data = await response.json();
  if (!response.ok) throw new Error(`OpenAI API: ${data.error?.message || response.status}`);
  const text = data.output
    ?.flatMap(item => item.content || [])
    ?.filter(item => item.type === "output_text")
    ?.map(item => item.text)
    ?.join("\n");
  if (!text) throw new Error("OpenAI không trả về văn bản.");
  return text;
}

async function gemini(prompt) {
  const response = await fetch("https://generativelanguage.googleapis.com/v1beta/interactions", {
    method: "POST",
    headers: {
      "x-goog-api-key": process.env.GEMINI_API_KEY,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      model: GEMINI_MODEL,
      input: prompt
    })
  });
  const data = await response.json();
  if (!response.ok) throw new Error(`Gemini API: ${data.error?.message || response.status}`);
  const text = (data.steps || [])
    .filter(step => step.type === "model_output")
    .flatMap(step => step.content || [])
    .filter(part => part.type === "text")
    .map(part => part.text)
    .join("\n");
  if (!text) throw new Error("Gemini chưa trả về văn bản. Kiểm tra model/API response.");
  return text;
}

app.get("/health", (_req, res) => res.json({ ok: true, service: "dual-ai-debate" }));

app.post("/debate", async (req, res) => {
  try {
    requireKeys();
    const question = String(req.body?.question || "").trim();
    if (question.length < 5 || question.length > 4000) {
      return res.status(400).json({ error: "Câu hỏi phải dài từ 5 đến 4000 ký tự." });
    }

    const base = `Người dùng hỏi: ${question}\n\nTrả lời bằng tiếng Việt. Không bịa số liệu, nguồn hoặc sự kiện. Nếu thiếu dữ liệu, hãy nói rõ điều chưa biết.`;

    const aInitial = await openai(
      `Bạn là AI A – NGƯỜI ỦNG HỘ (Advocate). Hãy đưa ra lập luận mạnh nhất cho hướng tiếp cận mà bạn cho là đúng. Viết 3-5 câu, có logic và dẫn chứng nếu biết chắc. Chỉ rõ giả định quan trọng.\n${base}`
    );

    const bCritique1 = await gemini(
      `Bạn là AI B – NGƯỜI PHẢN BIỆN (Challenger). Phản biện trực tiếp từng điểm trong lập luận của AI A. Tìm lỗ hổng, rủi ro, ngoại lệ; không phản đối vô căn cứ. Nếu A đúng ở điểm nào, hãy thừa nhận.\n${base}\nLập luận AI A:\n${aInitial}`
    );

    const aResponse = await openai(
      `Bạn là AI A – NGƯỜI ỦNG HỘ. Hãy phản hồi phản biện của B. Thừa nhận điểm hợp lý, sửa điểm yếu, bảo vệ điểm còn vững. Không né tránh.\n${base}\nLập luận ban đầu của A:\n${aInitial}\nPhản biện của B:\n${bCritique1}`
    );

    const bCritique2 = await gemini(
      `Bạn là AI B – NGƯỜI PHẢN BIỆN. Đây là vòng cuối. Đánh giá câu trả lời mới của A, chỉ nêu bất đồng còn có cơ sở và điều kiện khiến kết luận thay đổi. Nếu đã đồng ý, nói rõ.\n${base}\nA ban đầu:\n${aInitial}\nB phản biện:\n${bCritique1}\nA phản hồi:\n${aResponse}`
    );

    const finalAnswer = await openai(
      `Bạn là người tổng hợp trung lập. Hãy đưa ra đáp án tốt nhất dựa trên toàn bộ tranh luận. Không thiên vị A hoặc B. Nêu: (1) kết luận trực tiếp, (2) điểm hai bên thống nhất, (3) bất đồng còn lại và mức độ chắc chắn, (4) khuyến nghị thực tế. Không bịa nguồn hay số liệu. Trả lời tiếng Việt, rõ ràng.\n${base}\nAI A ban đầu:\n${aInitial}\nAI B phản biện vòng 1:\n${bCritique1}\nAI A phản hồi:\n${aResponse}\nAI B phản biện vòng 2:\n${bCritique2}`
    );

    res.json({
      a_initial: aInitial,
      b_critique_1: bCritique1,
      a_response: aResponse,
      b_critique_2: bCritique2,
      final_answer: finalAnswer
    });
  } catch (error) {
    console.error(error);
    res.status(502).json({ error: error.message || "Lỗi xử lý tranh luận." });
  }
});

app.listen(PORT, () => console.log(`DUAL AI DEBATE server listening on ${PORT}`));
