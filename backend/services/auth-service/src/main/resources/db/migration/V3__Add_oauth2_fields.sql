-- Add OAuth2 fields to users table
ALTER TABLE users
ADD COLUMN IF NOT EXISTS auth_provider VARCHAR(20) DEFAULT 'LOCAL',
ADD COLUMN IF NOT EXISTS provider_user_id VARCHAR(255),
ADD COLUMN IF NOT EXISTS profile_picture VARCHAR(500);

-- Make password nullable for OAuth users
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;

-- Create index on auth_provider and provider_user_id
CREATE INDEX IF NOT EXISTS idx_users_provider 
ON users(auth_provider, provider_user_id);

-- Add unique constraint for OAuth provider + user_id combination
ALTER TABLE users 
ADD CONSTRAINT unique_oauth_provider 
UNIQUE (auth_provider, provider_user_id);

-- Add comment
COMMENT ON COLUMN users.auth_provider IS 'Authentication provider: LOCAL, GOOGLE, FACEBOOK';
COMMENT ON COLUMN users.provider_user_id IS 'User ID from OAuth provider';
COMMENT ON COLUMN users.profile_picture IS 'URL to user profile picture';
