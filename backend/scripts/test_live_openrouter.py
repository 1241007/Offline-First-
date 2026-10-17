import asyncio
import httpx
from app.services.openrouter_service import openrouter_service

async def main():
    print(f"Testing OpenRouterService...")
    print(f"Is Configured: {openrouter_service.is_configured}")
    print(f"Primary Model: {openrouter_service._model_name}")
    print(f"Base URL: {openrouter_service._base_url}")
    
    # Direct HTTP check to OpenRouter /auth/key or /models to see key status and credits
    headers = openrouter_service._get_headers()
    async with httpx.AsyncClient() as client:
        try:
            auth_resp = await client.get(f"{openrouter_service._base_url}/auth/key", headers=headers)
            print(f"Auth check status: {auth_resp.status_code}")
            print(f"Auth response: {auth_resp.text}")
        except Exception as e:
            print(f"Auth check error: {e}")

    try:
        res = await openrouter_service.generate_response(
            history=[{"role": "user", "content": "Hello, what is photosynthesis? Explain in 1 sentence."}],
            explanation_mode="general",
        )
        print("GENERATE RESPONSE SUCCESS:")
        print(res)
    except Exception as exc:
        print(f"GENERATE RESPONSE ERROR: {type(exc).__name__}: {exc}")

if __name__ == "__main__":
    asyncio.run(main())
