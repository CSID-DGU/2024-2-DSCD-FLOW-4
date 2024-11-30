# app/services/token_service.py
import jwt
import time
import os
from dotenv import load_dotenv
import logging

logger = logging.getLogger(__name__)

class TokenService:
    def __init__(self):
        load_dotenv()
        self.api_key = os.getenv('LIVEKIT_API_KEY')
        self.api_secret = os.getenv('LIVEKIT_API_SECRET')

    def generate_token(self, identity: str, room_name: str = "default-room") -> str:
        try:
            now = int(time.time())
            claims = {
                "exp": now + (60 * 24 * 3600),  # 60일
                "iss": self.api_key,
                "nbf": now,
                "sub": identity,
                "video": {
                    "room": room_name,
                    "room_join": True,
                    "canPublish": True,
                    "canSubscribe": True,
                    "canPublishData": True
                }
            }
            
            token = jwt.encode(claims, self.api_secret, algorithm="HS256")
            logger.debug(f"Generated token for {identity} in room {room_name}")
            
            # 토큰 검증
            try:
                decoded = jwt.decode(token, self.api_secret, algorithms=["HS256"])
                logger.debug(f"Token validated successfully: {decoded}")
            except jwt.InvalidTokenError as e:
                logger.error(f"Token validation failed: {e}")
                raise
                
            return token
            
        except Exception as e:
            logger.error(f"Token generation failed: {e}")
            raise

    # app/services/token_service.py
def verify_token_details(self, token: str) -> dict:
    try:
        decoded = jwt.decode(token, self.api_secret, algorithms=["HS256"])
        
        # 토큰 상세 정보 로깅
        logger.debug("Token verification details:")
        logger.debug(f"Issuer: {decoded.get('iss')}")
        logger.debug(f"Subject: {decoded.get('sub')}")
        logger.debug(f"Room: {decoded.get('video', {}).get('room')}")
        logger.debug(f"Permissions: {decoded.get('video')}")
        
        # 만료 시간 확인
        exp = decoded.get('exp')
        if exp:
            exp_datetime = datetime.datetime.fromtimestamp(exp)
            now = datetime.datetime.now()
            logger.debug(f"Token expires at: {exp_datetime}")
            logger.debug(f"Time until expiration: {exp_datetime - now}")
            
        return {
            "is_valid": True,
            "details": decoded,
            "expires_at": exp_datetime if exp else None
        }
    except jwt.ExpiredSignatureError:
        logger.error("Token has expired")
        return {"is_valid": False, "error": "Token expired"}
    except jwt.InvalidTokenError as e:
        logger.error(f"Invalid token: {e}")
        return {"is_valid": False, "error": str(e)}
    except Exception as e:
        logger.error(f"Token verification failed: {e}")
        return {"is_valid": False, "error": str(e)}