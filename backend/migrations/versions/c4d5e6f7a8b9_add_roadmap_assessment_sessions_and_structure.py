"""add_roadmap_assessment_sessions_and_structure

Revision ID: c4d5e6f7a8b9
Revises: b2c3d4e5f6a7
Create Date: 2026-10-09 16:35:00.000000

"""
from typing import Sequence, Union
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql


# revision identifiers, used by Alembic.
revision: str = 'c4d5e6f7a8b9'
down_revision: Union[str, None] = 'b2c3d4e5f6a7'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # 1. Add structure column to roadmaps
    op.add_column(
        'roadmaps',
        sa.Column('structure', postgresql.JSONB(astext_type=sa.Text()).with_variant(sa.JSON(), 'sqlite'), nullable=True)
    )

    # 2. Create roadmap_assessment_sessions table
    op.create_table(
        'roadmap_assessment_sessions',
        sa.Column('id', sa.String(length=36), nullable=False),
        sa.Column('user_id', sa.String(length=255), nullable=False),
        sa.Column('state', sa.String(length=50), nullable=False),
        sa.Column('goal', sa.String(length=255), nullable=True),
        sa.Column('target_level', sa.String(length=50), nullable=True),
        sa.Column('target_timeline', sa.String(length=100), nullable=True),
        sa.Column('weekly_hours', sa.Float(), nullable=True),
        sa.Column('collected_profile', postgresql.JSONB(astext_type=sa.Text()).with_variant(sa.JSON(), 'sqlite'), nullable=True),
        sa.Column('messages', postgresql.JSONB(astext_type=sa.Text()).with_variant(sa.JSON(), 'sqlite'), nullable=True),
        sa.Column('pending_quiz', postgresql.JSONB(astext_type=sa.Text()).with_variant(sa.JSON(), 'sqlite'), nullable=True),
        sa.Column('roadmap_id', sa.String(length=36), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(['roadmap_id'], ['roadmaps.id'], ondelete='SET NULL'),
        sa.ForeignKeyConstraint(['user_id'], ['users.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index('ix_roadmap_assessments_user_id', 'roadmap_assessment_sessions', ['user_id'], unique=False)
    op.create_index('ix_roadmap_assessments_user_state', 'roadmap_assessment_sessions', ['user_id', 'state'], unique=False)


def downgrade() -> None:
    op.drop_index('ix_roadmap_assessments_user_state', table_name='roadmap_assessment_sessions')
    op.drop_index('ix_roadmap_assessments_user_id', table_name='roadmap_assessment_sessions')
    op.drop_table('roadmap_assessment_sessions')
    op.drop_column('roadmaps', 'structure')
