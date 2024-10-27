from livekit import api
from flask import Flask, jsonify

# API 키와 비밀 키를 직접 입력
LIVEKIT_API_KEY = 'your_api_key'  # 여기에 실제 API Key 입력
LIVEKIT_API_SECRET = 'your_api_secret'  # 여기에 실제 API Secret 입력

app = Flask(__name__)

@app.route('/getToken/<identity>')
def get_token(identity):
    token = api.AccessToken(LIVEKIT_API_KEY, LIVEKIT_API_SECRET) \
        .with_identity(identity) \
        .with_grants(api.VideoGrants(room_join=True, room="my-room"))
    return jsonify({'token': token.to_jwt()})

if __name__ == '__main__':
    app.run(debug=True)
