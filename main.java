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
     * Event listener interfaces
     */
    public interface MemoryCreatedListener {
        void onMemoryCreated(String creator, int memoryId, String content, long timestamp, String memoryHash);
    }
    
    public interface MilestoneReachedListener {
        void onMilestoneReached(int milestone, long timestamp);
    }
    
    /**
     * Constructor - no parameters needed, everything pre-populated
     * @param initialFunding Minimum 0.01 ether required
     */
    public BaseOnchainMemoryVault(BigDecimal initialFunding) {
        BigDecimal minimumFunding = new BigDecimal("10000000000000000"); // 0.01 ether in wei
        if (initialFunding.compareTo(minimumFunding) < 0) {
            throw new IllegalArgumentException("Initial funding required (minimum 0.01 ether)");
        }
        totalValueLocked = initialFunding;
        System.out.println("Vault initialized: " + VAULT_NAME);
        System.out.println("Genesis timestamp: " + GENESIS_TIMESTAMP);
        System.out.println("Vault seed: " + VAULT_SEED);
    }
    
    /**
     * Create a new onchain memory
     * @param creator The creator's address
     * @param content The memory content (max 280 characters)
     * @param payment Payment amount in wei
     * @return The created memory
     */
    public Memory createMemory(String creator, String content, BigDecimal payment) {
        // Validation
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("Content cannot be empty");
        }
        if (content.length() > MAX_MEMORY_LENGTH) {
            throw new IllegalArgumentException("Content too long (max " + MAX_MEMORY_LENGTH + " characters)");
        }
        if (payment.compareTo(MEMORY_COST_WEI) < 0) {
            throw new IllegalArgumentException("Insufficient payment (minimum " + MEMORY_COST_WEI + " wei)");
        }
        if (totalMemories >= VAULT_CAPACITY) {
            throw new IllegalStateException("Vault at capacity");
        }
        
        // Generate hash
        long currentTimestamp = Instant.now().getEpochSecond();
        String contentHash = keccak256(content + currentTimestamp + creator);
        
        if (memoryExists.contains(contentHash)) {
            throw new IllegalArgumentException("Duplicate memory detected");
        }
        
        // Create memory
        int memoryId = totalMemories;
        Memory newMemory = new Memory(creator, content, currentTimestamp, memoryId, contentHash);
        
        // Store memory
        memories.add(newMemory);
        creatorMemories.computeIfAbsent(creator, k -> new ArrayList<>()).add(memoryId);
        memoryExists.add(contentHash);
        
        // Update creator stats
        if (!isCreator.contains(creator)) {
            isCreator.add(creator);
            uniqueCreators.add(creator);
            totalCreators++;
        }
        
        creatorMemoryCount.put(creator, creatorMemoryCount.getOrDefault(creator, 0) + 1);
        totalMemories++;
        totalValueLocked = totalValueLocked.add(payment);
        
        // Check milestones
        checkMilestones();
        
        // Emit event
        for (MemoryCreatedListener listener : memoryCreatedListeners) {
            listener.onMemoryCreated(creator, memoryId, content, currentTimestamp, contentHash);
        }
        
        return newMemory;
    }
    
    /**
     * Get a specific memory by ID
     * @param memoryId The memory ID to retrieve
     * @return The memory object
     */
    public Memory getMemory(int memoryId) {
        if (memoryId < 0 || memoryId >= memories.size()) {
            throw new IllegalArgumentException("Memory does not exist");
        }
        return memories.get(memoryId);
    }
    
    /**
     * Get all memories created by an address
     * @param creator The creator address
     * @return List of memory IDs
     */
    public List<Integer> getCreatorMemories(String creator) {
        return new ArrayList<>(creatorMemories.getOrDefault(creator, new ArrayList<>()));
    }
    
    /**
     * Get total number of memories
     * @return Total memories count
     */
    public int getTotalMemories() {
        return totalMemories;
    }
    
    /**
     * Vault statistics
     */
    public static class VaultStats {
        public final int totalMemories;
        public final int totalCreators;
        public final BigDecimal totalValueLocked;
        public final int capacityRemaining;
        public final boolean milestone1;
        public final boolean milestone2;
        public final boolean milestone3;
        
        public VaultStats(int totalMemories, int totalCreators, BigDecimal totalValueLocked,
                         int capacityRemaining, boolean milestone1, boolean milestone2, boolean milestone3) {
            this.totalMemories = totalMemories;
            this.totalCreators = totalCreators;
            this.totalValueLocked = totalValueLocked;
            this.capacityRemaining = capacityRemaining;
            this.milestone1 = milestone1;
            this.milestone2 = milestone2;
            this.milestone3 = milestone3;
        }
        
        @Override
        public String toString() {
            return String.format("VaultStats{memories=%d, creators=%d, valueLocked=%s, remaining=%d, m1=%s, m2=%s, m3=%s}",
                totalMemories, totalCreators, totalValueLocked, capacityRemaining, milestone1, milestone2, milestone3);
        }
    }
    
    /**
     * Get vault statistics
     * @return Vault statistics object
     */
    public VaultStats getVaultStats() {
        return new VaultStats(
            totalMemories,
            totalCreators,
            totalValueLocked,
            VAULT_CAPACITY - totalMemories,
            milestone1Reached,
            milestone2Reached,
            milestone3Reached
        );
    }
    
    /**
     * Get all unique creators
     * @return List of creator addresses
     */
    public List<String> getAllCreators() {
        return new ArrayList<>(uniqueCreators);
    }
    
    /**
     * Get recent memories (last N memories)
     * @param count Number of recent memories to retrieve
     * @return List of recent memories
     */
    public List<Memory> getRecentMemories(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Count must be greater than 0");
        }
        int start = totalMemories > count ? totalMemories - count : 0;
        int end = totalMemories;
        return new ArrayList<>(memories.subList(start, end));
    }
    
    /**
     * Internal function to check and trigger milestones
     */
    private void checkMilestones() {
        if (!milestone1Reached && totalMemories >= MILESTONE_1) {
            milestone1Reached = true;
            long timestamp = Instant.now().getEpochSecond();
            for (MilestoneReachedListener listener : milestoneReachedListeners) {
                listener.onMilestoneReached(MILESTONE_1, timestamp);
            }
        }
        if (!milestone2Reached && totalMemories >= MILESTONE_2) {
            milestone2Reached = true;
            long timestamp = Instant.now().getEpochSecond();
            for (MilestoneReachedListener listener : milestoneReachedListeners) {
                listener.onMilestoneReached(MILESTONE_2, timestamp);
            }
        }
        if (!milestone3Reached && totalMemories >= MILESTONE_3) {
            milestone3Reached = true;
            long timestamp = Instant.now().getEpochSecond();
            for (MilestoneReachedListener listener : milestoneReachedListeners) {
                listener.onMilestoneReached(MILESTONE_3, timestamp);
            }
        }
    }
    
    /**
     * Get memory by hash (for verification)
     * @param hash The memory hash to search for
     * @return Optional containing memory ID if found
     */
    public Optional<Integer> getMemoryByHash(String hash) {
        for (int i = 0; i < memories.size(); i++) {
            if (memories.get(i).getMemoryHash().equals(hash)) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }
    
    /**
     * Verify a memory's integrity
     * @param memoryId The memory ID to verify
     * @return True if memory is valid
     */
    public boolean verifyMemory(int memoryId) {
        if (memoryId < 0 || memoryId >= memories.size()) {
            throw new IllegalArgumentException("Memory does not exist");
        }
        Memory mem = memories.get(memoryId);
        String computedHash = keccak256(mem.getContent() + mem.getTimestamp() + mem.getCreator());
        return computedHash.equals(mem.getMemoryHash());
    }
    
    /**
     * Add funds to vault
     * @param amount Amount in wei
     */
    public void addFunds(BigDecimal amount) {
        totalValueLocked = totalValueLocked.add(amount);
    }
    
    /**
     * Add event listener for memory creation
     * @param listener The listener to add
     */
    public void addMemoryCreatedListener(MemoryCreatedListener listener) {
        memoryCreatedListeners.add(listener);
    }
    
    /**
     * Add event listener for milestone reached
     * @param listener The listener to add
     */
