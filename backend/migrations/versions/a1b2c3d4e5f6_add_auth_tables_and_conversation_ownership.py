"""add_auth_tables_and_conversation_ownership

Revision ID: a1b2c3d4e5f6
Revises: fd5a2e32997d
Create Date: 2026-10-05 12:00:00.000000

"""
import logging
import uuid
from datetime import datetime, timezone
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy import text

revision: str = 'a1b2c3d4e5f6'
down_revision: Union[str, None] = 'fd5a2e32997d'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None

logger = logging.getLogger("alembic.migration")

# Non-functional placeholder — not a valid bcrypt hash prefix, can never authenticate
_MIGRATED_PLACEHOLDER = "$MIGRATED_PLACEHOLDER$"


def upgrade() -> None:
    conn = op.get_bind()

    # -----------------------------------------------------------------------
    # 1. Create users table
    # -----------------------------------------------------------------------
    op.create_table(
        'users',
        sa.Column('id', sa.String(36), nullable=False),
        sa.Column('email', sa.String(255), nullable=False),
        sa.Column('mobile', sa.String(50), nullable=True),
        sa.Column('hashed_password', sa.String(255), nullable=False),
        sa.Column('is_active', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('is_verified', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('email'),
    )
    op.create_index('ix_users_email', 'users', ['email'], unique=True)
    op.create_index('ix_users_mobile', 'users', ['mobile'], unique=True)

    # -----------------------------------------------------------------------
    # 2. Create refresh_tokens table
    # -----------------------------------------------------------------------
    op.create_table(
        'refresh_tokens',
        sa.Column('id', sa.String(36), nullable=False),
        sa.Column('user_id', sa.String(36), nullable=False),
        sa.Column('token_hash', sa.String(64), nullable=False),
        sa.Column('session_family_id', sa.String(36), nullable=False),
        sa.Column('expires_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('revoked', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(['user_id'], ['users.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('token_hash'),
    )
    op.create_index('ix_refresh_tokens_user_id', 'refresh_tokens', ['user_id'])
    op.create_index('ix_refresh_tokens_token_hash', 'refresh_tokens', ['token_hash'], unique=True)
    op.create_index('ix_refresh_tokens_session_family', 'refresh_tokens', ['session_family_id'])

    # -----------------------------------------------------------------------
    # 3. Create password_reset_tokens table
    # -----------------------------------------------------------------------
    op.create_table(
        'password_reset_tokens',
        sa.Column('id', sa.String(36), nullable=False),
        sa.Column('user_id', sa.String(36), nullable=False),
        sa.Column('token_hash', sa.String(64), nullable=False),
        sa.Column('expires_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('used', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(['user_id'], ['users.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('token_hash'),
    )
    op.create_index('ix_prt_user_id', 'password_reset_tokens', ['user_id'])
    op.create_index('ix_prt_token_hash', 'password_reset_tokens', ['token_hash'], unique=True)

    # -----------------------------------------------------------------------
    # 4. Safe orphan handling: inspect user_profiles before adding FK
    # -----------------------------------------------------------------------
    # Determine orphaned user_profiles (user_id not yet in users)
    profile_rows = conn.execute(
        text("SELECT user_id, email, mobile FROM user_profiles")
    ).fetchall()
    existing_user_ids = {
        row[0] for row in conn.execute(text("SELECT id FROM users")).fetchall()
    }
    orphaned = [r for r in profile_rows if r[0] not in existing_user_ids]

    if orphaned:
        logger.warning(
            f"Found {len(orphaned)} orphaned user_profiles record(s) with no matching users row."
        )
        # Check for unsafe orphans (missing email)
        unsafe = [r for r in orphaned if not r[1] or not r[1].strip()]
        if unsafe:
            ids = [r[0] for r in unsafe]
            raise RuntimeError(
                f"MIGRATION STOPPED: {len(unsafe)} orphaned user_profiles row(s) have NULL/empty "
                f"email and cannot be safely migrated. Manual resolution required for user_id(s): "
                f"{ids}"
            )

        # Count related data per orphaned user_id for the log
        for row in orphaned:
            uid = row[0]
            course_count = conn.execute(
                text("SELECT COUNT(*) FROM user_course_progress WHERE user_id = :uid"),
                {"uid": uid}
            ).scalar() or 0
            lesson_count = conn.execute(
                text("SELECT COUNT(*) FROM user_lesson_progress WHERE user_id = :uid"),
                {"uid": uid}
            ).scalar() or 0
            roadmap_count = conn.execute(
                text("SELECT COUNT(*) FROM roadmaps WHERE user_id = :uid"),
                {"uid": uid}
            ).scalar() or 0
            logger.warning(
                f"  Orphan user_id={uid} email={row[1]!r}: "
                f"course_progress={course_count}, lesson_progress={lesson_count}, "
                f"roadmaps={roadmap_count}, conversations=N/A (no user_id column yet)"
            )

        # Insert placeholder users rows — email from profile, no usable password
        now = datetime.now(timezone.utc)
        for row in orphaned:
            uid, email, mobile = row[0], row[1].strip().lower(), row[2]
            conn.execute(
                text(
                    "INSERT INTO users (id, email, mobile, hashed_password, "
                    "is_active, is_verified, created_at, updated_at) "
                    "VALUES (:id, :email, :mobile, :hashed_password, "
                    "true, false, :created_at, :updated_at)"
                ),
                {
                    "id": uid,
                    "email": email,
                    "mobile": mobile,
                    "hashed_password": _MIGRATED_PLACEHOLDER,
                    "created_at": now,
                    "updated_at": now,
                }
            )
            logger.warning(
                f"  Inserted placeholder users row for orphan user_id={uid} "
                f"(password is non-functional; user must reset via forgot-password)"
            )

    # -----------------------------------------------------------------------
    # 5. Add FK constraint from user_profiles.user_id → users.id
    # -----------------------------------------------------------------------
    op.create_foreign_key(
        'fk_user_profiles_user_id',
        'user_profiles',
        'users',
        ['user_id'],
        ['id'],
        ondelete='CASCADE',
    )

    # -----------------------------------------------------------------------
    # 6. Add user_id column to conversations (nullable — existing rows keep NULL)
    # -----------------------------------------------------------------------
    op.add_column(
        'conversations',
        sa.Column('user_id', sa.String(36), nullable=True)
    )
    op.create_index('ix_conversations_user_id', 'conversations', ['user_id'])


def downgrade() -> None:
    # Remove conversations.user_id index and column
    op.drop_index('ix_conversations_user_id', table_name='conversations')
    op.drop_column('conversations', 'user_id')

    # Remove FK from user_profiles before dropping users
    op.drop_constraint('fk_user_profiles_user_id', 'user_profiles', type_='foreignkey')

    # Drop auth tables (cascade handles FK deps)
    op.drop_index('ix_prt_token_hash', table_name='password_reset_tokens')
    op.drop_index('ix_prt_user_id', table_name='password_reset_tokens')
    op.drop_table('password_reset_tokens')

    op.drop_index('ix_refresh_tokens_session_family', table_name='refresh_tokens')
    op.drop_index('ix_refresh_tokens_token_hash', table_name='refresh_tokens')
    op.drop_index('ix_refresh_tokens_user_id', table_name='refresh_tokens')
    op.drop_table('refresh_tokens')

    op.drop_index('ix_users_mobile', table_name='users')
    op.drop_index('ix_users_email', table_name='users')
    op.drop_table('users')
