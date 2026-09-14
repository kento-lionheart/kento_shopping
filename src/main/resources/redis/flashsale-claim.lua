-- KEYS[1] stock   KEYS[2] inflight   KEYS[3] claim hash   KEYS[4] claims stream
-- ARGV[1] quantity   ARGV[2] claimId   ARGV[3] saleId   ARGV[4] userId   ARGV[5] claim TTL seconds
-- Returns remaining stock, or -1 insufficient, -3 not active, -4 quantity out of range.
local stock = tonumber(redis.call('GET', KEYS[1]))
if stock == nil then return -3 end
local qty = tonumber(ARGV[1])
if qty == nil or qty < 1 or qty > 3 then return -4 end
if stock < qty then return -1 end

redis.call('DECRBY', KEYS[1], qty)
redis.call('INCRBY', KEYS[2], qty)
redis.call('HSET', KEYS[3], 'status', 'PROCESSING', 'saleId', ARGV[3], 'userId', ARGV[4], 'quantity', ARGV[1])
redis.call('EXPIRE', KEYS[3], ARGV[5])
redis.call('XADD', KEYS[4], '*', 'claimId', ARGV[2], 'saleId', ARGV[3], 'userId', ARGV[4], 'quantity', ARGV[1])
return stock - qty
