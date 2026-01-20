"""
A2A SerpAPI Agent - Real-time Hotel Search using SerpAPI Google Hotels API

This agent implements the A2A protocol and provides real hotel search
capabilities using SerpAPI's Google Hotels API.
"""

import os
import json
import uuid
import httpx
import logging
from datetime import datetime, timedelta
from typing import Any, Dict, List, Optional
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from dotenv import load_dotenv

# Configure logging
logging.basicConfig(
    level=logging.DEBUG,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger("serpapi-agent")

# Load environment variables
load_dotenv()

SERPAPI_API_KEY = os.getenv("SERPAPI_API_KEY", "")
SERPAPI_BASE_URL = "https://serpapi.com/search"

app = FastAPI(
    title="A2A SerpAPI Hotel Agent",
    description="Real-time hotel search using SerpAPI Google Hotels API",
    version="1.0.0"
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Agent Card - describes this agent's capabilities
AGENT_CARD = {
    "name": "serpapi-hotel-agent",
    "description": "A specialized agent for searching real hotel data using Google Hotels via SerpAPI",
    "url": f"http://localhost:{os.getenv('PORT', '8083')}",
    "version": "1.0.0",
    "capabilities": {
        "streaming": False,
        "pushNotifications": False
    },
    "skills": [
        {
            "id": "search-hotels-live",
            "name": "Search Hotels Live",
            "description": "Search for real hotels using Google Hotels API. Returns live pricing and availability.",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "destination": {
                        "type": "string",
                        "description": "City or destination to search for hotels (e.g., 'Paris', 'New York')"
                    },
                    "checkInDate": {
                        "type": "string",
                        "description": "Check-in date in YYYY-MM-DD format"
                    },
                    "checkOutDate": {
                        "type": "string",
                        "description": "Check-out date in YYYY-MM-DD format"
                    },
                    "adults": {
                        "type": "integer",
                        "description": "Number of adults (default: 2)"
                    },
                    "currency": {
                        "type": "string",
                        "description": "Currency code (default: USD)"
                    },
                    "minPrice": {
                        "type": "integer",
                        "description": "Minimum price filter"
                    },
                    "maxPrice": {
                        "type": "integer",
                        "description": "Maximum price filter"
                    },
                    "minRating": {
                        "type": "number",
                        "description": "Minimum rating filter (e.g., 4.0)"
                    }
                },
                "required": ["destination"]
            }
        },
        {
            "id": "get-hotel-details",
            "name": "Get Hotel Details",
            "description": "Get detailed information about a specific hotel including amenities, reviews, and photos.",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "propertyToken": {
                        "type": "string",
                        "description": "The property token from search results"
                    },
                    "checkInDate": {
                        "type": "string",
                        "description": "Check-in date in YYYY-MM-DD format"
                    },
                    "checkOutDate": {
                        "type": "string",
                        "description": "Check-out date in YYYY-MM-DD format"
                    }
                },
                "required": ["propertyToken"]
            }
        }
    ],
    "defaultInputModes": ["text"],
    "defaultOutputModes": ["text"]
}

# In-memory task storage
tasks: Dict[str, Dict[str, Any]] = {}


class JsonRpcRequest(BaseModel):
    jsonrpc: str = "2.0"
    id: Any
    method: str
    params: Optional[Dict[str, Any]] = None


def create_json_rpc_response(id: Any, result: Any) -> Dict[str, Any]:
    return {
        "jsonrpc": "2.0",
        "id": id,
        "result": result
    }


def create_json_rpc_error(id: Any, code: int, message: str, data: Any = None) -> Dict[str, Any]:
    error = {"code": code, "message": message}
    if data:
        error["data"] = data
    return {
        "jsonrpc": "2.0",
        "id": id,
        "error": error
    }


async def search_hotels_live(params: Dict[str, Any]) -> Dict[str, Any]:
    """Search for hotels using SerpAPI Google Hotels API"""
    
    logger.info("=" * 60)
    logger.info("SEARCH HOTELS LIVE - REQUEST RECEIVED")
    logger.info("=" * 60)
    logger.debug(f"Input params: {json.dumps(params, indent=2)}")
    
    destination = params.get("destination", "")
    check_in = params.get("checkInDate")
    check_out = params.get("checkOutDate")
    adults = params.get("adults", 2)
    currency = params.get("currency", "USD")
    min_price = params.get("minPrice")
    max_price = params.get("maxPrice")
    min_rating = params.get("minRating")
    
    # Default dates if not provided (tomorrow + 2 nights)
    if not check_in:
        check_in = (datetime.now() + timedelta(days=1)).strftime("%Y-%m-%d")
    if not check_out:
        check_out = (datetime.now() + timedelta(days=3)).strftime("%Y-%m-%d")
    
    if not SERPAPI_API_KEY:
        logger.error("SerpAPI API key not configured!")
        return {
            "success": False,
            "error": "SerpAPI API key not configured. Please set SERPAPI_API_KEY environment variable.",
            "hotels": []
        }
    
    # Build SerpAPI request
    serpapi_params = {
        "engine": "google_hotels",
        "q": destination,
        "check_in_date": check_in,
        "check_out_date": check_out,
        "adults": adults,
        "currency": currency,
        "api_key": SERPAPI_API_KEY,
        "hl": "en",
        "gl": "us"
    }
    
    # Add price filters if provided
    if min_price is not None or max_price is not None:
        if min_price and max_price:
            serpapi_params["price"] = f"{min_price},{max_price}"
        elif min_price:
            serpapi_params["min_price"] = min_price
        elif max_price:
            serpapi_params["max_price"] = max_price
    
    # Log request (mask API key)
    safe_params = {k: ("***MASKED***" if k == "api_key" else v) for k, v in serpapi_params.items()}
    logger.info(f"SerpAPI Request URL: {SERPAPI_BASE_URL}")
    logger.info(f"SerpAPI Request Params: {json.dumps(safe_params, indent=2)}")
    
    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            logger.info("Sending request to SerpAPI...")
            response = await client.get(SERPAPI_BASE_URL, params=serpapi_params)
            logger.info(f"SerpAPI Response Status: {response.status_code}")
            response.raise_for_status()
            data = response.json()
        
        # Log response summary
        logger.info(f"SerpAPI Response Keys: {list(data.keys())}")
        if "search_metadata" in data:
            logger.debug(f"Search Metadata: {json.dumps(data['search_metadata'], indent=2)}")
        
        # Extract hotel results
        properties = data.get("properties", [])
        logger.info(f"Found {len(properties)} properties in response")
        
        # Transform to our format
        hotels = []
        for idx, prop in enumerate(properties[:10]):  # Limit to 10 results
            logger.debug(f"Processing property {idx + 1}: {prop.get('name', 'Unknown')}")
            
            # Log raw images data
            raw_images = prop.get("images", [])
            logger.debug(f"  Raw images count: {len(raw_images)}")
            if raw_images:
                logger.debug(f"  First image object: {json.dumps(raw_images[0], indent=4)}")
            
            hotel = {
                "name": prop.get("name", "Unknown"),
                "description": prop.get("description", ""),
                "propertyToken": prop.get("property_token", ""),
                "rating": prop.get("overall_rating"),
                "reviewCount": prop.get("reviews"),
                "stars": prop.get("hotel_class"),
                "price": None,
                "pricePerNight": None,
                "totalPrice": None,
                "currency": currency,
                "location": {
                    "address": prop.get("address", ""),
                    "neighborhood": prop.get("neighborhood", ""),
                    "latitude": prop.get("gps_coordinates", {}).get("latitude"),
                    "longitude": prop.get("gps_coordinates", {}).get("longitude")
                },
                "amenities": prop.get("amenities", []),
                "images": [img.get("thumbnail") or img.get("original_image") for img in prop.get("images", [])[:6] if img.get("thumbnail") or img.get("original_image")],
                "checkIn": check_in,
                "checkOut": check_out
            }
            
            # Log extracted images
            logger.debug(f"  Extracted images: {hotel['images']}")
            
            # Extract pricing
            rate_per_night = prop.get("rate_per_night")
            if rate_per_night:
                hotel["pricePerNight"] = rate_per_night.get("extracted_lowest")
                hotel["price"] = rate_per_night.get("lowest", "")
            
            total_rate = prop.get("total_rate")
            if total_rate:
                hotel["totalPrice"] = total_rate.get("extracted_lowest")
            
            # Apply rating filter if specified
            if min_rating and hotel["rating"]:
                if hotel["rating"] < min_rating:
                    logger.debug(f"  Skipped due to rating filter (rating={hotel['rating']}, min={min_rating})")
                    continue
            
            hotels.append(hotel)
        
        result = {
            "success": True,
            "destination": destination,
            "checkIn": check_in,
            "checkOut": check_out,
            "hotelCount": len(hotels),
            "hotels": hotels,
            "source": "Google Hotels via SerpAPI"
        }
        
        logger.info("=" * 60)
        logger.info("SEARCH HOTELS LIVE - RESPONSE")
        logger.info("=" * 60)
        logger.info(f"Success: True, Hotels found: {len(hotels)}")
        for h in hotels:
            logger.info(f"  - {h['name']}: {len(h.get('images', []))} images, price={h.get('price')}")
        
        return result
        
    except httpx.HTTPStatusError as e:
        logger.error(f"SerpAPI HTTP Error: {e.response.status_code}")
        logger.error(f"Response body: {e.response.text[:500] if e.response.text else 'N/A'}")
        return {
            "success": False,
            "error": f"SerpAPI request failed: {e.response.status_code}",
            "hotels": []
        }
    except Exception as e:
        logger.exception(f"Unexpected error in search_hotels_live: {str(e)}")
        return {
            "success": False,
            "error": f"Failed to search hotels: {str(e)}",
            "hotels": []
        }


async def get_hotel_details(params: Dict[str, Any]) -> Dict[str, Any]:
    """Get detailed information about a specific hotel"""
    
    logger.info("=" * 60)
    logger.info("GET HOTEL DETAILS - REQUEST RECEIVED")
    logger.info("=" * 60)
    logger.debug(f"Input params: {json.dumps(params, indent=2)}")
    
    property_token = params.get("propertyToken", "")
    check_in = params.get("checkInDate")
    check_out = params.get("checkOutDate")
    
    if not property_token:
        logger.warning("Property token is required but not provided")
        return {
            "success": False,
            "error": "Property token is required"
        }
    
    if not SERPAPI_API_KEY:
        logger.error("SerpAPI API key not configured!")
        return {
            "success": False,
            "error": "SerpAPI API key not configured"
        }
    
    # Default dates if not provided
    if not check_in:
        check_in = (datetime.now() + timedelta(days=1)).strftime("%Y-%m-%d")
    if not check_out:
        check_out = (datetime.now() + timedelta(days=3)).strftime("%Y-%m-%d")
    
    serpapi_params = {
        "engine": "google_hotels",
        "property_token": property_token,
        "check_in_date": check_in,
        "check_out_date": check_out,
        "api_key": SERPAPI_API_KEY,
        "hl": "en",
        "gl": "us"
    }
    
    # Log request (mask API key)
    safe_params = {k: ("***MASKED***" if k == "api_key" else v) for k, v in serpapi_params.items()}
    logger.info(f"SerpAPI Request URL: {SERPAPI_BASE_URL}")
    logger.info(f"SerpAPI Request Params: {json.dumps(safe_params, indent=2)}")
    
    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            logger.info("Sending request to SerpAPI for hotel details...")
            response = await client.get(SERPAPI_BASE_URL, params=serpapi_params)
            logger.info(f"SerpAPI Response Status: {response.status_code}")
            response.raise_for_status()
            data = response.json()
        
        logger.info(f"SerpAPI Response Keys: {list(data.keys())}")
        logger.debug(f"Full response (truncated): {json.dumps(data, indent=2)[:2000]}")
        
        # Extract detailed info
        result = {
            "success": True,
            "hotel": data,
            "source": "Google Hotels via SerpAPI"
        }
        
        logger.info("=" * 60)
        logger.info("GET HOTEL DETAILS - RESPONSE SUCCESS")
        logger.info("=" * 60)
        
        return result
        
    except Exception as e:
        logger.exception(f"Error getting hotel details: {str(e)}")
        return {
            "success": False,
            "error": f"Failed to get hotel details: {str(e)}"
        }


async def execute_skill(skill_id: str, params: Dict[str, Any]) -> Dict[str, Any]:
    """Execute a skill and return the result"""
    
    logger.info(f"Executing skill: {skill_id}")
    
    if skill_id == "search-hotels-live":
        return await search_hotels_live(params)
    elif skill_id == "get-hotel-details":
        return await get_hotel_details(params)
    else:
        logger.warning(f"Unknown skill requested: {skill_id}")
        return {
            "success": False,
            "error": f"Unknown skill: {skill_id}"
        }


@app.get("/.well-known/agent.json")
async def get_agent_card():
    """Return the Agent Card for discovery"""
    return JSONResponse(content=AGENT_CARD)


@app.post("/a2a")
async def handle_a2a(request: Request):
    """Handle A2A JSON-RPC requests"""
    
    logger.info("=" * 60)
    logger.info("A2A REQUEST RECEIVED")
    logger.info("=" * 60)
    
    try:
        body = await request.json()
        logger.debug(f"Raw request body: {json.dumps(body, indent=2)[:1000]}")
        rpc_request = JsonRpcRequest(**body)
    except Exception as e:
        logger.error(f"Failed to parse A2A request: {str(e)}")
        return JSONResponse(
            content=create_json_rpc_error(None, -32700, f"Parse error: {str(e)}")
        )
    
    method = rpc_request.method
    params = rpc_request.params or {}
    request_id = rpc_request.id
    
    logger.info(f"A2A Method: {method}")
    logger.info(f"A2A Request ID: {request_id}")
    
    # Handle different A2A methods
    if method == "tasks/send":
        # Extract skill and parameters from the message
        message = params.get("message", {})
        skill_id = params.get("skillId", "")
        
        logger.info(f"Skill ID: {skill_id}")
        
        # Extract data from message parts
        task_params = {}
        parts = message.get("parts", [])
        for part in parts:
            if part.get("type") == "data":
                task_params = part.get("data", {})
                break
        
        logger.info(f"Task params: {json.dumps(task_params, indent=2)}")
        
        # Create task
        task_id = str(uuid.uuid4())
        logger.info(f"Created task ID: {task_id}")
        
        # Execute the skill
        result = await execute_skill(skill_id, task_params)
        
        # Store task
        task = {
            "id": task_id,
            "status": {"state": "completed"},
            "artifacts": [
                {
                    "parts": [
                        {
                            "type": "data",
                            "data": result,
                            "mimeType": "application/json"
                        }
                    ]
                }
            ]
        }
        tasks[task_id] = task
        
        logger.info(f"Task {task_id} completed, returning response")
        logger.debug(f"Task result success: {result.get('success')}, hotel count: {result.get('hotelCount', 'N/A')}")
        
        return JSONResponse(content=create_json_rpc_response(request_id, task))
    
    elif method == "tasks/get":
        task_id = params.get("id")
        logger.info(f"tasks/get for task_id: {task_id}")
        if task_id in tasks:
            return JSONResponse(
                content=create_json_rpc_response(request_id, tasks[task_id])
            )
        logger.warning(f"Task not found: {task_id}")
        return JSONResponse(
            content=create_json_rpc_error(request_id, -32000, "Task not found")
        )
    
    elif method == "tasks/cancel":
        task_id = params.get("id")
        logger.info(f"tasks/cancel for task_id: {task_id}")
        if task_id in tasks:
            tasks[task_id]["status"]["state"] = "canceled"
            return JSONResponse(
                content=create_json_rpc_response(request_id, tasks[task_id])
            )
        logger.warning(f"Task not found: {task_id}")
        return JSONResponse(
            content=create_json_rpc_error(request_id, -32000, "Task not found")
        )
    
    else:
        logger.warning(f"Unknown A2A method: {method}")
        return JSONResponse(
            content=create_json_rpc_error(request_id, -32601, f"Method not found: {method}")
        )


@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "agent": "serpapi-hotel-agent",
        "serpapi_configured": bool(SERPAPI_API_KEY)
    }


if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("PORT", "8083"))
    host = os.getenv("HOST", "0.0.0.0")
    print(f"Starting A2A SerpAPI Agent on {host}:{port}")
    uvicorn.run(app, host=host, port=port)
