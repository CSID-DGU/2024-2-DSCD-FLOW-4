
import os
import json
import speech_recognition as sr
from openai import OpenAI
from pathlib import Path
from playsound import playsound

# OpenAI 설정
client = OpenAI(api_key=os.getenv('OPENAI_KEY'))

# JSON 파일 로드
with open('recommended_recipes.json', 'r', encoding='utf-8') as file:
    recipe_data = json.load(file)

# 현재 진행 중인 레시피 데이터 가져오기
current_recipe = recipe_data["recommended_recipes"][0]
recipe_name = current_recipe["recipe_name"]
ingredients = ", ".join(current_recipe["ingredients"])
steps = current_recipe["steps"]

# 단계 및 파일 인덱스 초기화
current_step_index = 0  # 현재 조리 단계
file_index = 1  # 음성 파일 번호 (전역 변수로 관리)

# 초기 메시지용 TTS: 텍스트를 음성 파일로 변환
def text_to_speech_initial(text):
    global file_index
    speech_file_path = f"speech{file_index}.mp3"  # 순차적으로 파일 이름 생성
    file_index += 1  # 파일 번호 증가
    response = client.audio.speech.create(
        model="tts-1",
        voice="alloy",
        input=text
    )
    response.stream_to_file(Path(speech_file_path))
    print(f"초기 메시지 음성 파일 생성 완료: {speech_file_path}")
    playsound(speech_file_path)  # 음성 파일 재생

# 단계별 TTS: 텍스트를 음성 파일로 변환
def text_to_speech(text):
    global file_index
    speech_file_path = f"speech{file_index}.mp3"  # 순차적으로 파일 이름 생성
    file_index += 1  # 파일 번호 증가
    response = client.audio.speech.create(
        model="tts-1",
        voice="alloy",
        input=text
    )
    response.stream_to_file(Path(speech_file_path))
    print(f"음성 파일 생성 완료: {speech_file_path}")
    playsound(speech_file_path)  # 음성 파일 재생

# LLM을 사용하여 질문에 대한 답변을 생성하는 함수
def generate_answer(user_input):
    response = client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[
            {
                "role": "system",
                "content": (
                    "You are a helpful cooking assistant. "
                    "Always keep your responses concise, limited to two sentences or less."
                )
            },
            {"role": "user", "content": user_input}
        ]
    )
    return response.choices[0].message.content

# 다음 단계 요청을 확인하는 함수
def get_next_step(user_input):
    global current_step_index
    if "다음" in user_input:
        current_step_index += 1
        if current_step_index < len(steps):
            return f"다음 단계 안내입니다: {steps[current_step_index]}"
        else:
            return "모든 조리 단계를 안내드렸습니다. 맛있게 드세요!"
    elif "다시" in user_input:
        return f"현재 단계 다시 안내드립니다: {steps[current_step_index]}"
    else:
        return generate_answer(user_input)  # 질문에 대한 LLM 답변 생성

# 마이크로부터 음성을 텍스트로 변환
def speech_to_text_from_microphone():
    recognizer = sr.Recognizer()
    with sr.Microphone() as source:
        print("음성을 입력하세요...")
        try:
            audio = recognizer.listen(source, timeout=7)
            text = recognizer.recognize_google(audio, language="ko-KR")
            print(f"음성 인식 결과: {text}")
            return text
        except sr.WaitTimeoutError:
            print("시간 초과: 음성이 입력되지 않았습니다.")
            return ""
        except sr.UnknownValueError:
            print("음성을 이해할 수 없습니다. 다시 시도하세요.")
            return ""

# 대화 시작
def interactive_cooking():
    global current_step_index
    # 초기 메시지 출력
    initial_message = (
        f"안녕하세요 방구석 미슐랭 회원님, 오늘 만들어볼 요리는 '{recipe_name}'입니다. "
        f"필요한 재료는 {ingredients}입니다. 이제 조리 단계를 안내해드릴까요?"
    )
    print(initial_message)
    text_to_speech_initial(initial_message)  # 초기 메시지 음성 파일 생성 및 재생
    
    while current_step_index < len(steps):
        user_input = speech_to_text_from_microphone()
        if not user_input:
            continue  # 음성 입력 실패 시 다시 시도

        response = get_next_step(user_input)
        print(f"응답 (텍스트): {response}")
        text_to_speech(response) 

# 프로그램 실행
interactive_cooking()
