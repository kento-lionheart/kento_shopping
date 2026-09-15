-- KEYS[1] claim hash   KEYS[2] inflight   KEYS[3] stock
-- ARGV[1] PAID | FAILED   ARGV[2] quantity   ARGV[3] message   ARGV[4] orderId
-- Returns 1 when the claim moved out of PROCESSING, 0 when it had already been finalised.
if redis.call('HGET', KEYS[1], 'status') ~= 'PROCESSING' then return 0 end
local qty = tonumber(ARGV[2])

redis.call('HSET', KEYS[1], 'status', ARGV[1])
if ARGV[3] ~= '' then redis.call('HSET', KEYS[1], 'message', ARGV[3]) end
if ARGV[4] ~= '' then redis.call('HSET', KEYS[1], 'orderId', ARGV[4]) end
redis.call('DECRBY', KEYS[2], qty)
if ARGV[1] == 'FAILED' and redis.call('EXISTS', KEYS[3]) == 1 then
    redis.call('INCRBY', KEYS[3], qty)
end
return 1
