from flask import Flask, request, jsonify
from flask_sqlalchemy import SQLAlchemy
import boto3
import requests
import os
import io
import base64
import logging
from PIL import Image
from dotenv import load_dotenv
import jwt
import time
import datetime
import uvicorn
import json
import requests

load_dotenv()

# 환경 변수 로드 및 초기 설정
LIVEKIT_API_KEY = os.environ.get("LIVEKIT_API_KEY")
LIVEKIT_API_SECRET = os.environ.get("LIVEKIT_API_SECRET")
OPENAI_API_KEY = os.environ.get("OPENAI_API_KEY")
GOOGLE_API_KEY = os.environ.get("GOOGLE_API_KEY")
SEARCH_ENGINE_ID = os.environ.get("SEARCH_ENGINE_ID")
API_ENDPOINT = os.environ.get("CHATGPT_API_URL")

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("agent")

# Flask 및 데이터베이스 설정
app = Flask(__name__)
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///mydatabase.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False

db = SQLAlchemy(app)

# 데이터베이스 모델 정의
class Ingredient(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(100), nullable=False)
    image_url = db.Column(db.String(200), nullable=True)

# 데이터베이스 모델 정의
class Recipe(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    food_name = db.Column(db.String(100), nullable=False)
    cooking_time = db.Column(db.String(50), nullable=False)
    image_url = db.Column(db.String(200), nullable=False)
    instructions = db.Column(db.Text, nullable=True)

# 데이터베이스 초기화
with app.app_context():
    db.create_all()
        
        
def analyze_fridge_contents(receipt_url):
    """온라인 영수증 URL에서 식자재를 분석합니다.""" 
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {OPENAI_API_KEY}"
    }

    prompt = """당신은 식자재 인식 전문가입니다. 주어진 온라인 영수증 사진을 분석하여 구매한 식자재의 이름을 배열 형태로 반환해야 합니다.
    예를 들어, {"ingredients": ["양파", "당근"]}와 같은 형식으로 응답해 주세요. 응답에 'json'이라는 단어가 포함되지 않도록 해주세요.
    이 영수증은 사용자가 최근에 구매한 식자재 목록을 포함하고 있습니다. 브랜드명과 부가적인 라벨 정보는 제거하고, 식재료 이름에만 집중합니다."""

    payload = {
        "model": "gpt-4o",
        "messages": [
            {"role": "user", "content": prompt},
            {
                "role": "user",
                "content": [
                    {"type": "text", "text": "이 이미지를 분석해 주세요."},
                    {"type": "image_url", "image_url": {"url": receipt_url}}
                ]
            }
        ],
        "max_tokens": 2000
    }
    return _analyze_contents(headers, payload)

def analyze_fridge_contents_simple(food_url): 
    """이미지 URL에서 식자재를 분석합니다."""
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {OPENAI_API_KEY}"
    }

    prompt = """당신은 식자재 인식 전문가입니다. 사용자가 업로드한 이미지를 분석하여 구매한 식자재의 이름을 배열 형태로 반환해야 합니다.
    예를 들어, {"ingredients": ["양파", "당근"]} 와 같은 형식으로 응답해 주세요. 응답에 'json'이라는 단어가 포함되지 않도록 해주세요.
    이 이미지는 마트에서 구매한 다양한 식자재를 보여줍니다."""

    payload = {
        "model": "gpt-4o",
        "messages": [
            {"role": "user", "content": prompt},
            {
                "role": "user",
                "content": [
                    {"type": "text", "text": "이 이미지를 분석해 주세요."},
                    {"type": "image_url", "image_url": {"url": food_url}}
                ]
            }
        ],
        "max_tokens": 2000
    }
    return _analyze_contents(headers, payload)

def _analyze_contents(headers, payload):
    """식자재 분석 요청을 처리하는 공통 함수."""
    try:
        response = requests.post(API_ENDPOINT, headers=headers, json=payload)
        response.raise_for_status()
        
        ingredients_json = response.json()['choices'][0]['message']['content']
        
        if ingredients_json.startswith("json"):
            ingredients_json = ingredients_json[4:].strip()  # 'json' 제거 후 공백 제거

        return json.loads(ingredients_json)  # JSON 파싱
    except requests.exceptions.RequestException as e:
        logger.error(f"식자재 이미지 분석 중 오류 발생: {e}")
        return None
    except json.JSONDecodeError as e:
        logger.error(f"JSON 파싱 중 오류 발생: {e}")
        return None

def search_image(query):
    """이미지 검색을 수행합니다."""
    url = "https://www.googleapis.com/customsearch/v1"
    params = {
        'q': query,
        'cx': os.environ.get("SEARCH_ENGINE_ID"),
        'searchType': 'image',
        'key': os.environ.get("GOOGLE_API_KEY"),
        'num': 1
    }
    
    try:
        response = requests.get(url, params=params)
        response.raise_for_status()
        results = response.json()
        if 'items' in results:
            return results['items'][0]['link']
    except requests.exceptions.RequestException as e:
        logger.error(f"Image search error: {e}")
    return None

@app.route('/api/image/food', methods=['POST'])
def upload_food_image():
    """이미지 URL을 사용하여 식자재를 분석하고 저장합니다."""
    data = request.get_json()
    image_url = data.get('image_url')

    if not image_url:
        return jsonify({"error": "No image URL provided"}), 400

    ingredients_response = analyze_fridge_contents_simple(image_url)

    if ingredients_response is None:
        return jsonify({"error": "Failed to analyze ingredients from image"}), 500

    if isinstance(ingredients_response, dict) and "ingredients" in ingredients_response:
        ingredients_list = ingredients_response["ingredients"]
    else:
        return jsonify({"error": "No ingredients found in the response"}), 500

    # 기존 재료 삭제
    db.session.query(Ingredient).delete()
    db.session.commit()

    saved_images = []

    for ingredient in ingredients_list:
        image_url = search_image(ingredient)
        if image_url:
            new_ingredient = Ingredient(name=ingredient, image_url=image_url)
            db.session.add(new_ingredient)
            saved_images.append({"name": ingredient, "image_url": image_url})

    db.session.commit()
    
    return jsonify({"saved_ingredients": saved_images}), 200 

@app.route('/api/image/receipt', methods=['POST'])
def upload_receipt_image():
    """영수증 이미지 URL을 사용하여 식자재를 분석하고 저장합니다."""
    data = request.get_json()
    receipt_url = data.get('receipt_url')

    if not receipt_url:
        return jsonify({"error": "No receipt URL provided"}), 400

    ingredients_response = analyze_fridge_contents(receipt_url)

    if ingredients_response is None:
        return jsonify({"error": "Failed to analyze ingredients from receipt"}), 500

    if isinstance(ingredients_response, dict) and "ingredients" in ingredients_response:
        ingredients_list = ingredients_response["ingredients"]
    else:
        return jsonify({"error": "No ingredients found in the response"}), 500

    # 기존 재료 삭제
    db.session.query(Ingredient).delete()
    db.session.commit()

    saved_images = []

    for ingredient in ingredients_list:
        image_url = search_image(ingredient)
        if image_url:
            new_ingredient = Ingredient(name=ingredient, image_url=image_url)
            db.session.add(new_ingredient)
            saved_images.append({"name": ingredient, "image_url": image_url})

    db.session.commit()
    
    return jsonify({"saved_ingredients": saved_images}), 200 


@app.route('/api/ingredients', methods=['GET'])
def get_ingredients():
    """저장된 모든 재료와 이미지를 반환합니다."""
    ingredients = Ingredient.query.all()
    images = {ingredient.name: ingredient.image_url for ingredient in ingredients}
    result = [ingredient.name for ingredient in ingredients]

    return jsonify({"images": images, "ingredients": result}), 200


###################################################
# 데이터베이스 모델 정의
@app.route('/api/recipe/details/<int:id>', methods=['GET'])
def get_recipe_details(id):
    """특정 레시피의 세부 정보를 가져옵니다."""
    recipe = Recipe.query.get(id)  # ID로 레시피 조회
    if not recipe:
        return jsonify({"error": "Recipe not found"}), 404

    # 레시피 세부 정보 반환 (foodName, cookingTime, image, instructions 포함)
    return jsonify({
        "recipe": { 
            "foodName": recipe.food_name,
            "cookingTime": recipe.cooking_time,
            "image": recipe.image_url,
            "instructions": recipe.instructions  # 조리 순서 포함
        }
    }), 200

@app.route('/api/recipe', methods=['POST'])
def get_recipes():
    """사용자가 제공한 재료를 기반으로 3가지 레시피를 추천하고, 기존 재료 DB를 업데이트합니다."""
    data = request.get_json()
    ingredients_list = data.get('ingredients')

    if not ingredients_list or not isinstance(ingredients_list, list):
        return jsonify({"error": "Invalid input. Expected a list of ingredients."}), 400

    # 레시피 추천
    recipe_response = recipe_recommend(ingredients_list)

    if not recipe_response:
        return jsonify({"error": "Failed to recommend recipes"}), 500

    # 기존 재료 삭제 후 새 재료 추가
    db.session.query(Ingredient).delete()  # 기존 재료 삭제
    db.session.commit()

    saved_recipes = []
    for recipe in recipe_response[:3]:  # 처음 3개 레시피만 저장
        new_recipe = Recipe(
            food_name=recipe['foodName'],
            cooking_time=recipe['cookingTime'],
            image_url=recipe['image'],
            instructions=recipe.get('instructions')
        )
        db.session.add(new_recipe)
        saved_recipes.append(new_recipe)

    # 새 재료 DB에 추가
    for ingredient in ingredients_list:
        image_url = search_image(ingredient)  # 이미지 검색
        if image_url:
            new_ingredient = Ingredient(name=ingredient, image_url=image_url)
            db.session.add(new_ingredient)

    db.session.commit()

    return jsonify({
        "recipes": [
            {
                "foodName": recipe.food_name,
                "cookingTime": recipe.cooking_time,
                "image": recipe.image_url
            } for recipe in saved_recipes
        ]
    }), 200


def recipe_recommend(ingredients):
    """주어진 재료로 만들 수 있는 3가지 레시피를 추천합니다."""
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {OPENAI_API_KEY}"
    }
    
    # 프롬프트 수정: JSON 형식으로 반환 요청
    prompt = (
        f"다음 재료로 만들 수 있는 3가지의 레시피를 추천해줘: {', '.join(ingredients)}. "
        "각 레시피는 다음과 같은 형식으로 반환해줘:\n"
        '[{"foodName": "음식명", "cookingTime": "조리시간", "image": "음식 사진 URL", "instructions": "조리 순서"}]\n'
        "예시:\n"
        '[{"foodName": "스파게티", "cookingTime": "30분", "image": "https://example.com/spaghetti.jpg", "instructions": "1. 물을 끓인다. 2. 면을 삶는다."}]\n'
        "재료 목록은 출력하지 않아도 돼.\n"
        "이 레시피는 아무런 추가 설명 없이 JSON 형식으로만 반환해야 해."
        "응답에 'json'이라는 단어가 포함되지 않도록 해주세요."
    )

    payload = {
        "model": "gpt-4o",
        "messages": [
            {
                "role": "user",
                "content": prompt
            }
        ],
        "max_tokens": 1000
    }

    try:
        response = requests.post(API_ENDPOINT, headers=headers, json=payload)
        response.raise_for_status()
        recipes = response.json()['choices'][0]['message']['content']
        
        # JSON 형태로 변환
        return json.loads(recipes)  # JSON 파싱
    except requests.exceptions.RequestException as e:
        logger.error(f"레시피 추천 중 오류 발생: {e}")
        return None
    except json.JSONDecodeError as e:  
        logger.error(f"JSON 파싱 중 오류 발생: {e}, 응답 내용: {recipes}")  # 오류 발생 시 응답 내용 로그
        return None


# Main Application Entry Point
if __name__ == "__main__":
    app.run(host="0.0.0.0", debug=True, port=8000)