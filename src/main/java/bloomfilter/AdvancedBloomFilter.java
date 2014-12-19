package bloomfilter;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.BitSet;
import java.util.Collection;
import java.util.Set;

/*
 *    xiangtao
 */
		
public class AdvancedBloomFilter<E> implements Serializable {
	private static final long serialVersionUID = 1L;
	private BitSet bitset;
    private int bitSetSize;
    private double bitsPerElement;
    private int expectedNumberOfFilterElements; 
    private int numberOfAddedElements; 
    private int k; 
    static final Charset charset = Charset.forName("UTF-8"); 
    static final String hashName = "MD5"; 
    static final MessageDigest digestFunction;
    static { 
        MessageDigest tmp;
        try {
            tmp = java.security.MessageDigest.getInstance(hashName);
        } catch (NoSuchAlgorithmException e) {
            tmp = null;
        }
        digestFunction = tmp;
    }
    @SuppressWarnings("unchecked")
	public void addKeywords(String keywords){
	 	for(String word : keywords.split(",")){
	 		this.add((E) word);
	 	}
    }
    @SuppressWarnings("unchecked")
    public void addKeywords(Set<String> keywords){
	 	for(String word : keywords){
	 		this.add((E) word.toLowerCase());
	 	}
    }
    public AdvancedBloomFilter() {
    	
    }
    
    public AdvancedBloomFilter(double c, int n, int k) {
      this.expectedNumberOfFilterElements = n;
      this.k = k;
      this.bitsPerElement = c;
      this.bitSetSize = (int)Math.ceil(c * n);
      numberOfAddedElements = 0;
      this.bitset = new BitSet(bitSetSize);
    }

    public AdvancedBloomFilter(int bitSetSize, int expectedNumberOElements) {
        this(bitSetSize / (double)expectedNumberOElements,
             expectedNumberOElements,
             (int) Math.round((bitSetSize / (double)expectedNumberOElements) * Math.log(2.0)));
    }
    public AdvancedBloomFilter(double falsePositiveProbability, int expectedNumberOfElements) {
        this(Math.ceil(-(Math.log(falsePositiveProbability) / Math.log(2))) / Math.log(2), // c = k / ln(2)
             expectedNumberOfElements,
             (int)Math.ceil(-(Math.log(falsePositiveProbability) / Math.log(2)))); // k = ceil(-log_2(false prob.))
    }

    public AdvancedBloomFilter(int bitSetSize, int expectedNumberOfFilterElements, int actualNumberOfFilterElements, BitSet filterData) {
        this(bitSetSize, expectedNumberOfFilterElements);
        this.bitset = filterData;
        this.numberOfAddedElements = actualNumberOfFilterElements;
    }

    public  int createHash(String val, Charset charset) {
        return createHash(val.getBytes(charset));
    }

    public  int createHash(String val) {
        return createHash(val, charset);
    }

    public int createHash(byte[] data) {
        return createHashes(data, 1)[0];
    }

    public  int[] createHashes(byte[] data, int hashes) {
        int[] result = new int[hashes];

        int k = 0;
        byte salt = 0;
        while (k < hashes) {
            byte[] digest;
            synchronized (digestFunction) {
                digestFunction.update(salt);
                salt++;
                digest = digestFunction.digest(data);                
            }
        
            for (int i = 0; i < digest.length/4 && k < hashes; i++) {
                int h = 0;
                for (int j = (i*4); j < (i*4)+4; j++) {
                    h <<= 8;
                    h |= ((int) digest[j]) & 0xFF;
                }
                result[k] = h;
                k++;
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AdvancedBloomFilter<E> other = (AdvancedBloomFilter<E>) obj;        
        if (this.expectedNumberOfFilterElements != other.expectedNumberOfFilterElements) {
            return false;
        }
        if (this.k != other.k) {
            return false;
        }
        if (this.bitSetSize != other.bitSetSize) {
            return false;
        }
        if (this.bitset != other.bitset && (this.bitset == null || !this.bitset.equals(other.bitset))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 61 * hash + (this.bitset != null ? this.bitset.hashCode() : 0);
        hash = 61 * hash + this.expectedNumberOfFilterElements;
        hash = 61 * hash + this.bitSetSize;
        hash = 61 * hash + this.k;
        return hash;
    }


    public double expectedFalsePositiveProbability() {
        return getFalsePositiveProbability(expectedNumberOfFilterElements);
    }

    public double getFalsePositiveProbability(double numberOfElements) {
        // (1 - e^(-k * n / m)) ^ k
        return Math.pow((1 - Math.exp(-k * (double) numberOfElements
                        / (double) bitSetSize)), k);

    }

    public double getFalsePositiveProbability() {
        return getFalsePositiveProbability(numberOfAddedElements);
    }


    public int getK() {
        return k;
    }

    public void clear() {
        bitset.clear();
        numberOfAddedElements = 0;
    }

    public int  countBitSetTrueNum() {
      return   bitset.cardinality();
    }
    
    public void add(E element) {
    	if(!this.contains(element))
        add(element.toString().getBytes(charset));
    }

    public void add(byte[] bytes) {
       int[] hashes = createHashes(bytes, k);
       for (int hash : hashes)
           bitset.set(Math.abs(hash % bitSetSize), true);
       numberOfAddedElements ++;
    }
    public void remove(E element) {
    	if(this.contains(element))
    	remove(element.toString().getBytes(charset));
     }

     public void remove(byte[] bytes) {
        int[] hashes = createHashes(bytes, k);
        for (int hash : hashes)
            bitset.set(Math.abs(hash % bitSetSize), false);
        numberOfAddedElements --;
     }
    public void addAll(Collection<? extends E> c) {
        for (E element : c)
            add(element);
    }
        
    public boolean contains(E element) {
        return contains(element.toString().getBytes(charset));
    }

    public boolean contains(byte[] bytes) {
        int[] hashes = createHashes(bytes, k);
        for (int hash : hashes) {
            if (!bitset.get(Math.abs(hash % bitSetSize))) {
                return false;
            }
        }
        return true;
    }

    public boolean containsAll(Collection<? extends E> c) {
        for (E element : c)
            if (!contains(element))
                return false;
        return true;
    }

    public boolean getBit(int bit) {
        return bitset.get(bit);
    }

    public void setBit(int bit, boolean value) {
        bitset.set(bit, value);
    }

    public BitSet getBitSet() {
        return bitset;
    }

    public int size() {
        return this.bitSetSize;
    }

    public int count() {
        return this.numberOfAddedElements;
    }

    public int getExpectedNumberOfElements() {
        return expectedNumberOfFilterElements;
    }

    public double getExpectedBitsPerElement() {
        return this.bitsPerElement;
    }

    public double getBitsPerElement() {
        return this.bitSetSize / (double)numberOfAddedElements;
    }
    
    public void saveBit(String filename){
    	File file = new File(filename);
    	try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file,false));
			oos.writeObject(bitset);
			oos.flush();
			oos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    }
    public void save(String filename){
    	File file = new File(filename);
    	try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
			oos.writeObject(this);
			oos.flush();
			oos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    @SuppressWarnings("rawtypes")
	public boolean read(String filename) throws FileNotFoundException, IOException, ClassNotFoundException{
    	File file = new File(filename);
    	ObjectInputStream ois = null;
    	@SuppressWarnings("rawtypes")
		AdvancedBloomFilter bloomfilter = null;
		try {
			ois = new ObjectInputStream(new FileInputStream(file));
			bloomfilter = (AdvancedBloomFilter)ois.readObject();
		   	ois.close();
		   	bitset = bloomfilter.getBitSet();
	    	bitsPerElement = bloomfilter.getExpectedBitsPerElement();
	    	k = bloomfilter.getK();
	    	numberOfAddedElements = bloomfilter.count();
	    	expectedNumberOfFilterElements = bloomfilter.getExpectedNumberOfElements();
	    	bitSetSize = bloomfilter.size();
	    	
		} catch (Exception e) {
	    	System.out.println("Can not find the file from : "+file);
	    	return false;
		}
    	
    	return true;
    }
    public void readBit(String filename) throws FileNotFoundException, IOException, ClassNotFoundException{
    	File file = new File(filename);
    	bitset.clear();
    	ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(new FileInputStream(file));
		} catch (Exception e) {
	    	System.out.println("can not find any file :"+file);
	    	return ;
		}
    	bitset = (BitSet)ois.readObject();
    	ois.close();
    }
    public boolean search (String word){
    	boolean flag = false;
    	if (this.contains((E) word))  flag = true;
    	return flag;
    }
    public boolean searchBySentence (String line){
    	
    	boolean flag = false;
    	for(String word : line.split("\\s+")){
        	if (this.contains((E) word))  flag = true;
    	}
    	return flag;
    }
   public boolean searchByWords (Set<String> wordSet){
    	
    	boolean flag = false;
    	for(String word : wordSet){
        	if (this.contains((E) word))  flag = true;
    	}
    	return flag;
    }
    public static void main(String agc[]){
        double falsePositiveProbability = 0.001;
    	int expectedSize = 100;
    	AdvancedBloomFilter<String> f = new AdvancedBloomFilter<String>(falsePositiveProbability, expectedSize);
    	String positionWord = "President,Managing,Director,salesman,Salesman,Vice,Executive,Manager,Officer,founder,specialist,CEO"+
		        	",CTO,Dean,Consultant,Assistant,CFO,Captain,Executive,Supervisor,Sales,Sale,Financial"+
		        	",Minister,Professor,Partner,COO,Leader,Chairman,co-founder,ree";
    	f.addKeywords(positionWord);
    	System.out.println(f.count());
        f.add("Consultant");
    	System.out.println(f.count());
    	f.remove("Consultant");
    	System.out.println(f.count());

    }
}