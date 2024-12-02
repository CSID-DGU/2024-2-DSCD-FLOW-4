# app/services/rtc_service.py
from livekit import rtc
from livekit.agents import AutoSubscribe, JobContext
import logging
import asyncio

logger = logging.getLogger(__name__)

class RTCService:
    def __init__(self):
        self.room = rtc.Room()
        self.connected = False

    async def setup_connection(self, ctx: JobContext):
        try:
            logger.debug(f"Starting RTC connection setup for room: {ctx.room_name}")
            
            # RTC 연결 설정
            await ctx.connect(auto_subscribe=AutoSubscribe.AUDIO_ONLY)
            
            @self.room.on("participant_connected")
            def on_participant_connected(participant):
                logger.debug(f"Participant connected: {participant.identity}")

            @self.room.on("connected")
            def on_connected():
                logger.debug("Room connected successfully")
                self.connected = True

            @self.room.on("disconnected")
            def on_disconnected():
                logger.debug("Room disconnected")
                self.connected = False

            @self.room.on("connection_state_changed")
            def on_state_changed(state: rtc.ConnectionState):
                logger.debug(f"Connection state changed to: {state}")

            # 룸 연결
            await self.room.connect(
                url=ctx.livekit_url,
                token=ctx.token,
                options=rtc.RoomOptions(
                    auto_subscribe=True,
                    dynacast=True
                )
            )
            
            # 연결 확인
            retry_count = 0
            while not self.connected and retry_count < 5:
                await asyncio.sleep(1)
                retry_count += 1
                logger.debug(f"Waiting for connection... attempt {retry_count}")

            if not self.connected:
                logger.error("Failed to establish connection after retries")
                return False

            logger.debug("RTC connection setup completed successfully")
            return True
            
        except Exception as e:
            logger.error(f"RTC connection error: {e}")
            return False

    async def disconnect(self):
        if self.connected:
            await self.room.disconnect()
            self.connected = False