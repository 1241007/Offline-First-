import asyncio
from sqlalchemy import text
from sqlalchemy.ext.asyncio import create_async_engine
from app.core.config import settings

async def check_policies():
    engine = create_async_engine(settings.async_database_url, pool_pre_ping=True)
    async with engine.connect() as conn:
        res = await conn.execute(text("""
            SELECT tablename, policyname, permissive, roles, cmd, qual, with_check
            FROM pg_policies
            WHERE tablename IN ('syllabus_documents', 'syllabus_chunks');
        """))
        policies = res.fetchall()
        print("RLS Policies:")
        for p in policies:
            print(p)
            
        # Check permissions for roles
        perm_res = await conn.execute(text("""
            SELECT grantee, table_name, privilege_type
            FROM information_schema.role_table_grants
            WHERE table_name IN ('syllabus_documents', 'syllabus_chunks')
            ORDER BY table_name, grantee, privilege_type;
        """))
        print("\nRole grants:")
        for g in perm_res.fetchall():
            print(g)
            
    await engine.dispose()

if __name__ == "__main__":
    asyncio.run(check_policies())
