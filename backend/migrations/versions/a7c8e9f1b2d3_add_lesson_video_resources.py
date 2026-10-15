"""Add video_url and video_provider to lessons table

Revision ID: a7c8e9f1b2d3
Revises: e3b8a1c92d54
Create Date: 2026-10-15 16:45:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'a7c8e9f1b2d3'
down_revision: Union[str, None] = 'e3b8a1c92d54'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Add optional video_url and video_provider columns to lessons table
    op.add_column('lessons', sa.Column('video_url', sa.Text(), nullable=True))
    op.add_column('lessons', sa.Column('video_provider', sa.String(length=100), nullable=True))


def downgrade() -> None:
    # Drop video_provider and video_url columns from lessons table
    op.drop_column('lessons', 'video_provider')
    op.drop_column('lessons', 'video_url')
