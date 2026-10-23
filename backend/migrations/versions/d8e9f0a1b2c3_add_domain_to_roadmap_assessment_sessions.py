"""Add domain column to roadmap_assessment_sessions

Revision ID: d8e9f0a1b2c3
Revises: c7d8e9f0a1b2
Create Date: 2026-10-19 22:50:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'd8e9f0a1b2c3'
down_revision: Union[str, None] = 'c7d8e9f0a1b2'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Add domain column to roadmap_assessment_sessions table
    op.add_column(
        'roadmap_assessment_sessions',
        sa.Column('domain', sa.String(length=100), nullable=True, server_default='Python')
    )


def downgrade() -> None:
    # Drop domain column from roadmap_assessment_sessions table
    op.drop_column('roadmap_assessment_sessions', 'domain')
