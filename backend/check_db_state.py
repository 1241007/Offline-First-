#!/usr/bin/env python3
"""Temporary script to verify database state before Phase 1 implementation"""

import asyncio
from sqlalchemy import text
from app.core.database import get_db


async def check_db_state():
    async for db in get_db():
        try:
            # Check conversations
            result = await db.execute(text("SELECT COUNT(*) FROM conversations"))
            conv_count = result.scalar()
            
            # Check messages
            result = await db.execute(text("SELECT COUNT(*) FROM messages"))
            msg_count = result.scalar()
            
            print(f"✅ Database connection successful")
            print(f"✅ Conversations table exists: {conv_count} rows")
            print(f"✅ Messages table exists: {msg_count} rows")
            
            # Check migration history
            result = await db.execute(text("SELECT version_num FROM alembic_version"))
            version = result.scalar()
            print(f"✅ Current migration version: {version}")
            
            return True
        except Exception as e:
            print(f"❌ Database check failed: {e}")
            return False


if __name__ == "__main__":
    success = asyncio.run(check_db_state())
    exit(0 if success else 1)
