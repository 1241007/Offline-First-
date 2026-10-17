import asyncio
import itertools
from app.services.openrouter_service import openrouter_service

async def run_experiment():
    print(f"=== OPENROUTER LIVE REPEATABILITY EXPERIMENT ===")
    print(f"Base URL: {openrouter_service._base_url}")
    print(f"Primary Configured Model: {openrouter_service._model_name}")
    print(f"Configured Max Tokens: {openrouter_service._max_tokens}")
    print(f"Configured Temperature: {openrouter_service._temperature}")
    print(f"Configured Seed: {openrouter_service._seed}")
    print(f"Configured Provider Order: {openrouter_service._provider_order}")

    prompt = "Explain Database Management Systems (DBMS) in three concise bullet points."
    history = [{"role": "user", "content": prompt}]
    
    outputs = []
    models_used = []
    providers_used = []
    finish_reasons = []

    for i in range(1, 6):
        print(f"\nExecuting Request {i} of 5...")
        try:
            resp = await openrouter_service.generate_response(
                history=history,
                explanation_mode="general",
            )
            model = getattr(openrouter_service, "last_model_used", "unknown")
            provider = getattr(openrouter_service, "last_provider", "unknown")
            finish_reason = getattr(openrouter_service, "last_finish_reason", "unknown")
            
            outputs.append(resp)
            models_used.append(model)
            providers_used.append(provider)
            finish_reasons.append(finish_reason)
            
            print(f"Request {i} SUCCESS:")
            print(f"  Model: {model}")
            print(f"  Provider: {provider}")
            print(f"  Finish Reason: {finish_reason}")
            print(f"  Char Length: {len(resp)}")
            print(f"  Preview: {repr(resp[:80])}...")
        except Exception as e:
            print(f"Request {i} FAILED: {type(e).__name__}: {e}")

    num_runs = len(outputs)
    print(f"\n==========================================")
    print(f"RESULTS FOR {num_runs} SUCCESSFUL RUNS:")
    print(f"==========================================")
    
    # 1. Exact match against Run 1
    exact_matches_against_run1 = sum(1 for out in outputs if out == outputs[0])
    match_rate_run1 = (exact_matches_against_run1 / num_runs) * 100.0 if num_runs else 0.0
    print(f"Exact Matches against Run 1: {exact_matches_against_run1}/{num_runs} ({match_rate_run1:.1f}%)")

    # 2. Pairwise comparisons across all 10 pairs
    pairs = list(itertools.combinations(range(num_runs), 2))
    matching_pairs = 0
    print(f"\nPairwise Comparison (Total {len(pairs)} pairs):")
    for idx1, idx2 in pairs:
        is_exact = (outputs[idx1] == outputs[idx2])
        if is_exact:
            matching_pairs += 1
        print(f"  Pair ({idx1+1}, {idx2+1}): {'EXACT MATCH' if is_exact else 'DIFFERENT'} (len1={len(outputs[idx1])}, len2={len(outputs[idx2])})")
    
    pairwise_match_rate = (matching_pairs / len(pairs)) * 100.0 if pairs else 0.0
    print(f"Pairwise Exact Match Count: {matching_pairs}/{len(pairs)} ({pairwise_match_rate:.1f}%)")

    # 3. Match Groups
    groups = {}
    for idx, out in enumerate(outputs, 1):
        found_group = False
        for g_rep, members in groups.items():
            if out == outputs[g_rep - 1]:
                members.append(idx)
                found_group = True
                break
        if not found_group:
            groups[idx] = [idx]
            
    print(f"\nDistinct Output Groups (Count: {len(groups)}):")
    for rep, members in groups.items():
        print(f"  Group {rep} (Runs {members}): Length={len(outputs[rep-1])} chars")
        print(f"    Sample: {repr(outputs[rep-1][:120])}...")

    # Full outputs dump
    print(f"\nFull Outputs Dump:")
    for idx, out in enumerate(outputs, 1):
        print(f"\n--- RUN {idx} ({models_used[idx-1]}, {providers_used[idx-1]}, {finish_reasons[idx-1]}, {len(out)} chars) ---")
        print(out)

if __name__ == "__main__":
    asyncio.run(run_experiment())
