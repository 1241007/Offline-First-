"""add_rag_profile_fields

Revision ID: e1f2a3b4c5d6
Revises: c4d5e6f7a8b9
Create Date: 2026-10-14 22:45:00.000000

"""
from typing import Sequence, Union
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'e1f2a3b4c5d6'
down_revision: Union[str, None] = 'c4d5e6f7a8b9'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        'user_profiles',
        sa.Column('board', sa.String(length=50), nullable=True)
    )
    op.add_column(
        'user_profiles',
        sa.Column('class_grade', sa.String(length=20), nullable=True)
    )
    op.add_column(
        'user_profiles',
        sa.Column('subject', sa.String(length=100), nullable=True)
    )
    op.add_column(
        'user_profiles',
        sa.Column('preferred_language', sa.String(length=20), nullable=True, server_default='en')
    )


def downgrade() -> None:
    op.drop_column('user_profiles', 'preferred_language')
    op.drop_column('user_profiles', 'subject')
    op.drop_column('user_profiles', 'class_grade')
    op.drop_column('user_profiles', 'board')
