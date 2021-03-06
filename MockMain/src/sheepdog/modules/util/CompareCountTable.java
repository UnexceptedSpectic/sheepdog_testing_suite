package sheepdog.modules.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

public class CompareCountTable
{
	public static final String[] LEVELS = { "phylum", "class", "order", "family", "genus" };
	
	public static void assertEquals(File classificationDirectory, File biolockJTable, String level) throws Exception
	{
		OtuWrapper wrapper = new OtuWrapper(biolockJTable);
		
		HashMap<String, HashMap<String,Long>> expectationMaps = getExpectationMaps(classificationDirectory, level);
		
		if( wrapper.getSampleNames().size() != expectationMaps.size())
			throw new Exception("Unequal sample size " + wrapper.getSampleNames().size() + " " + expectationMaps.size());
		
		for(int x=0; x < wrapper.getSampleNames().size();x++)
		{
			String sampleName = wrapper.getSampleNames().get(x);
			
			HashMap<String, Long> innerMap = expectationMaps.get(sampleName);
			expectationMaps.remove(sampleName);
			
			for( int y=0; y < wrapper.getOtuNames().size(); y++)
			{
				String taxaName = wrapper.getOtuNames().get(y);
				
				Long expectationVal = innerMap.get(taxaName);
				
				if( expectationVal == null)
					expectationVal = 0L;
				
				innerMap.remove(taxaName);
				
				Double tableVal = wrapper.getDataPointsUnnormalized().get(x).get(y);
				
				if( Math.abs(tableVal-expectationVal) > 0.00001)
					throw new Exception("Mismatch " + taxaName + " " + tableVal + " " + expectationVal);
				}
			
			if( innerMap.size() !=0)
				throw new Exception("Expecting empty map " + innerMap);
		}
		
		if( expectationMaps.size() != 0 )
			throw new Exception("Expecting empty map " + expectationMaps.size());
	}
	
	/*
	 * Outer key is sample is;  inner key is taxa id
	 */
	private static HashMap<String, HashMap<String,Long>> getExpectationMaps(File classificationDirectory, String level) 
				throws Exception
	{
		HashMap<String, HashMap<String,Long>>  map = new HashMap<>();
		
		String[] inFiles = classificationDirectory.list();
		
		for(String s : inFiles)
		{
			if(! s.endsWith("metadata.tsv"))
			{
				String sampleId = s.substring(s.lastIndexOf("_")+1, s.length()).replace(".tsv", "");
				
				if( map.containsKey(sampleId))
					throw new Exception("Duplicate sample ID " + sampleId);
				
				File inFile = new File(classificationDirectory.getAbsoluteFile()  +File.separator + s);
				map.put(sampleId, getExpectationMap(inFile, level));
			}
		}
		
		return map;
	}
	
	/*
	 * Outer key is sample is;  inner key is taxa id
	 */
	public static HashMap<String, HashMap<String,Long>> getExpectationMaps(List<File> inputFiles, String level) 
				throws Exception
	{
		HashMap<String, HashMap<String,Long>>  map = new HashMap<>();
		
		for(File inFile : inputFiles)
		{
			String fileName =inFile.getName();
			
			String sampleId = fileName.substring(fileName.lastIndexOf("_")+1, fileName.length()).replace(".tsv", "");
				
			map.put(sampleId, getExpectationMap(inFile, level));
		}
		
		return map;
	}
	
	private static HashMap<String, Long> getExpectationMap(File inFile, String level)
		throws Exception
	{
		//System.out.println("Expectation map for " + inFile.getAbsolutePath() + " " + level);
		HashMap<String, Long> map = new HashMap<>();
		
		BufferedReader reader = new BufferedReader( new FileReader( inFile));
		
		for(String s= reader.readLine(); s != null; s= reader.readLine())
		{
			String foundTaxa = null;
			StringTokenizer sToken = new StringTokenizer(s, "\t");
			
			if( sToken.countTokens() !=2 )
				throw new Exception("Expecting two tokens " + s );
			
			String[] splits =sToken.nextToken().split("\\|");
			
			for( String s2 : splits)
				if( s2.startsWith(level + "__") )
				{
					if( foundTaxa != null)
						throw new Exception("Duplicate levels" + level);
					
					foundTaxa = s2.replace(level + "__", "");
					
					Long oldVal = map.get(foundTaxa);
					
					if( oldVal == null)
						oldVal =0l;
					
					map.put(foundTaxa, Long.parseLong(sToken.nextToken()) + oldVal);
				}
			
			if( foundTaxa == null)
				throw new Exception("Could not find " + level + " " + s);
		}
		
		return map;
	}
	
	public static void main(String[] args) throws Exception
	{
		File classificationDirectory= 
			new File("C:\\sheepDog\\sheepdog_testing_suite\\MockMain\\pipelines\\reparseKraken2Parser_2_2019Jul29\\01_Kraken2Parser\\output");
		
		for( String taxa : LEVELS)
		{
			File inFile = 
					new File("C:\\sheepDog\\sheepdog_testing_suite\\MockMain\\pipelines\\reparseKraken2Parser_2019Jul24\\03_BuildTaxaTables\\output\\"+ 
								"reparseKraken2Parser_2019Jul24_taxaCount_" + taxa +  ".tsv");
			
			assertEquals(classificationDirectory, inFile, taxa);
			System.out.println("Pass " + taxa);
		}
		
		System.out.println("Global pass");
		
		/*
		 HashMap<String, HashMap<String,Long>> expectationMaps = 
				 getExpectationMaps(classificationDirectory, "genus");
		 
		 for(String s : expectationMaps.keySet())
			 System.out.println(s + " " + expectationMaps.get(s));
			 */
	}
}

