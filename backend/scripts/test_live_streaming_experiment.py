import asyncio
import itertools
from app.services.openrouter_service import openrouter_service

async def run_streaming_experiment():
    print(f"=== OPENROUTER LIVE STREAMING REPEATABILITY EXPERIMENT ===")
    prompt = "Explain Database Management Systems (DBMS) in three concise bullet points."
    history = [{"role": "user", "content": prompt}]
    
    outputs = []
    models_used = []
    finish_reasons = []

    for i in range(1, 6):
        chunks = []
        try:
            async for chunk in openrouter_service.generate_response_stream(
                history=history,
                explanation_mode="general",
            ):
                chunks.append(chunk)
            
            full_text = "".join(chunks).strip()
            model = getattr(openrouter_service, "last_model_used", "unknown")
            finish_reason = getattr(openrouter_service, "last_finish_reason", "unknown")
            
            outputs.append(full_text)
            models_used.append(model)
            finish_reasons.append(finish_reason)
            
            print(f"Stream Run {i} SUCCESS:")
            print(f"  Model: {model}")
            print(f"  Finish Reason: {finish_reason}")
            print(f"  Chunks Count: {len(chunks)}")
            print(f"  Total Chars: {len(full_text)}")
        except Exception as e:
            print(f"Stream Run {i} FAILED: {type(e).__name__}: {e}")

    num_runs = len(outputs)
    print(f"\n==========================================")
    print(f"STREAMING RESULTS FOR {num_runs} RUNS:")
    print(f"==========================================")
    
    exact_matches_against_run1 = sum(1 for out in outputs if out == outputs[0])
    print(f"Exact Matches against Run 1: {exact_matches_against_run1}/{num_runs}")

    pairs = list(itertools.combinations(range(num_runs), 2))
    matching_pairs = 0
    for idx1, idx2 in pairs:
        is_exact = (outputs[idx1] == outputs[idx2])
        if is_exact:
            matching_pairs += 1
        print(f"  Pair ({idx1+1}, {idx2+1}): {'EXACT MATCH' if is_exact else 'DIFFERENT'} (len1={len(outputs[idx1])}, len2={len(outputs[idx2])})")
    
    print(f"Pairwise Exact Match Count: {matching_pairs}/{len(pairs)} ({matching_pairs*100.0/len(pairs):.1f}%)" if pairs else "N/A")

    print(f"\nSample Output:\n{outputs[0] if outputs else 'None'}")

if __name__ == "__main__":
    asyncio.run(run_streaming_experiment())
