import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Base Onchain Memory Vault - Java Implementation
 * A decentralized, permanent onchain memory storage system
 * Aligned with Base ethos: everything onchain, community-driven, immutable
 */
public class BaseOnchainMemoryVault {
    
    // Pre-populated configuration values (unique random values)
    public static final int MAX_MEMORY_LENGTH = 280; // Base's character limit inspiration
    public static final BigDecimal MEMORY_COST_WEI = new BigDecimal("420000000000000"); // 0.00042 ether in wei
    public static final int VAULT_CAPACITY = 10000; // Maximum memories
    public static final long GENESIS_TIMESTAMP = 1738281600L; // Pre-set genesis time
    
    // Unique vault metadata
    public static final String VAULT_NAME = "Base Onchain Memory Vault #8472";
    public static final String VAULT_SYMBOL = "BOMV8472";
    public static final String VAULT_SEED = "8f3a7b2c9d4e1f6a5b8c7d2e9f4a1b6c8d3e7f2a9b4c1d6e8f3a7b2c9d4e1f6a";
    
    // Special milestone rewards (pre-populated thresholds)
    public static final int MILESTONE_1 = 100;
    public static final int MILESTONE_2 = 1000;
    public static final int MILESTONE_3 = 5000;
    
    // Memory storage
    private final List<Memory> memories = new CopyOnWriteArrayList<>();
    private final Map<String, List<Integer>> creatorMemories = new HashMap<>();
    private final Set<String> memoryExists = new HashSet<>();
    private final Map<String, Integer> creatorMemoryCount = new HashMap<>();
    
    // Community stats
    private int totalMemories = 0;
    private int totalCreators = 0;
    private BigDecimal totalValueLocked = BigDecimal.ZERO;
    private final List<String> uniqueCreators = new ArrayList<>();
    private final Set<String> isCreator = new HashSet<>();
    
    // Milestone tracking
    private boolean milestone1Reached = false;
    private boolean milestone2Reached = false;
    private boolean milestone3Reached = false;
    
    // Event listeners (simple implementation)
    private final List<MemoryCreatedListener> memoryCreatedListeners = new ArrayList<>();
    private final List<MilestoneReachedListener> milestoneReachedListeners = new ArrayList<>();
    
    /**
     * Memory data structure
     */
    public static class Memory {
        private final String creator;
        private final String content;
        private final long timestamp;
        private final int memoryId;
        private final String memoryHash;
        
        public Memory(String creator, String content, long timestamp, int memoryId, String memoryHash) {
            this.creator = creator;
            this.content = content;
            this.timestamp = timestamp;
            this.memoryId = memoryId;
            this.memoryHash = memoryHash;
        }
        
        public String getCreator() { return creator; }
        public String getContent() { return content; }
        public long getTimestamp() { return timestamp; }
        public int getMemoryId() { return memoryId; }
        public String getMemoryHash() { return memoryHash; }
        
        @Override
        public String toString() {
            return String.format("Memory{id=%d, creator=%s, content='%s', timestamp=%d, hash=%s}",
                memoryId, creator, content, timestamp, memoryHash);
        }
    }
    
    /**
