-- Sliding-window log.
-- KEYS[1] per-user sorted set   ARGV[1] now ms   ARGV[2] window ms   ARGV[3] limit   ARGV[4] unique member
-- Returns 1 when admitted, 0 when the window is full.
local now = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, now - window)
if redis.call('ZCARD', KEYS[1]) >= tonumber(ARGV[3]) then return 0 end
redis.call('ZADD', KEYS[1], now, ARGV[4])
redis.call('PEXPIRE', KEYS[1], window)
return 1
