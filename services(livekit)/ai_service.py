from livekit.plugins.openai import realtime
from livekit.agents.multimodal import MultimodalAgent
import logging

logger = logging.getLogger(__name__)

class AIService:
    def __init__(self):
        self.model = realtime.RealtimeModel(
            instructions="안녕하세요! 저는 당신의 요리를 도와드릴 요리 보조 AI입니다. 어떤 음식을 만들고 싶으신가요?",
            voice="shimmer",
            modalities=["audio", "text"]
        )
        self.agent = MultimodalAgent(model=self.model)

    async def start_session(self, room):
        try:
            self.agent.start(room)
            return True
        except Exception as e:
            logger.error(f"AI session error: {e}")
            return False

    async def stop_session(self):
        await self.agent.stop()