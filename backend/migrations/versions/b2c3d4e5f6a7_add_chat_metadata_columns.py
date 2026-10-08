"""add_chat_metadata_columns_and_user_memories

Revision ID: b2c3d4e5f6a7
Revises: a1b2c3d4e5f6
Create Date: 2026-10-08 19:15:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'b2c3d4e5f6a7'
down_revision: Union[str, None] = 'a1b2c3d4e5f6'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # 1. Add columns to conversations
    op.add_column('conversations', sa.Column('is_archived', sa.Boolean(), nullable=False, server_default='false'))
    op.add_column('conversations', sa.Column('is_pinned', sa.Boolean(), nullable=False, server_default='false'))
    op.add_column('conversations', sa.Column('draft_text', sa.String(length=4000), nullable=True, server_default=''))

    # 2. Add columns to messages
    op.add_column('messages', sa.Column('parent_id', sa.String(length=36), nullable=True))
    op.add_column('messages', sa.Column('is_edited', sa.Boolean(), nullable=False, server_default='false'))

    # 3. Create user_memories table
    op.create_table(
        'user_memories',
        sa.Column('id', sa.String(length=36), primary_key=True),
        sa.Column('user_id', sa.String(length=36), nullable=False),
        sa.Column('category', sa.String(length=50), nullable=False),
        sa.Column('content', sa.Text(), nullable=False),
        sa.Column('importance', sa.Float(), nullable=False, server_default='1.0'),
        sa.Column('confidence', sa.Float(), nullable=False, server_default='1.0'),
        sa.Column('source_conversation_id', sa.String(length=36), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('last_used_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('active', sa.Boolean(), nullable=False, server_default='true'),
    )
    op.create_index('ix_user_memories_user_id', 'user_memories', ['user_id'])
    op.create_index('ix_user_memories_user_active', 'user_memories', ['user_id', 'active'])


def downgrade() -> None:
    op.drop_index('ix_user_memories_user_active', table_name='user_memories')
    op.drop_index('ix_user_memories_user_id', table_name='user_memories')
    op.drop_table('user_memories')
    op.drop_column('messages', 'is_edited')
    op.drop_column('messages', 'parent_id')
    op.drop_column('conversations', 'draft_text')
    op.drop_column('conversations', 'is_pinned')
    op.drop_column('conversations', 'is_archived')
