package util;

import net.sf.jsqlparser.statement.select.PlainSelect;
import util.Constants.JoinMethod;
import util.Constants.SortMethod;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import model.IndexConfig;
import model.TableHistogram;
import model.Histogram;

import javax.swing.*;

/**
 * Singleton class to track and record states. For the operators to get info like schemas, files etc.
 * Created by Yufu Mo
 */
public class Catalog {
    private static Catalog instance = null;
    // store file location for different tables
    private Map<String, String> files = new HashMap<>();
    // store aliases map<alias, table name>
    private Map<String, String> aliases = new HashMap<>();
    // keep track of current schema map<alias, index>
    private Map<String, Integer> currentSchema = new HashMap<>();
    // store all the table-schema pairs
    private Map<String, Map<String, Integer>> schemas = new HashMap<>();
    // input path
    private String inputPath;
    // output path
    private String outputPath;

    private JoinMethod joinMethod = JoinMethod.TNLJ;
    private SortMethod sortMethod = SortMethod.IN_MEMORY;

    private int joinBlockSize = 0;
    private int sortBlockSize = 0;

    private boolean indexScan = false;

    // column order of the output tuple
    private List<String> attributeOrder;


    /**
     * private constructor for singleton class
     * initialize the input and output path
     * read in the schemas
     */
    private Catalog() {
        try {
            inputPath = Constants.SQLQURIES_PATH;
            outputPath = Constants.OUTPUT_PATH;
            System.out.println("Catalog initialize");
            System.out.println(Constants.SCHEMA_PATH);
            FileReader file = new FileReader(Constants.SCHEMA_PATH);
            BufferedReader br = new BufferedReader(file);
            String s = br.readLine();
            while (s != null) {
                String[] str = s.split("\\s+");
                // initiate aliases table map
                aliases.put(str[0], str[0]);

                files.put(str[0], Constants.DATA_PATH + str[0]);
                Map<String, Integer> schema = new HashMap<>();

                for (int i = 1; i < str.length; ++i) {
                    String field = str[0];
                    schema.put(field + "." + str[i], i - 1);
                }
                schemas.put(str[0], schema);
                s = br.readLine();
            }
            br.close();

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Files not found!");
        }

    }


    /**
     * store the
     */
    public void setAttributeOrder(PlainSelect plainSelect) {
        this.attributeOrder = new ArrayList<>();
        if (plainSelect.getSelectItems().get(0).toString() == "*") {
            List<String> tableList = new ArrayList<>();
            tableList.add(plainSelect.getFromItem().toString());
            if (plainSelect.getJoins() != null) {
                for (Object join : plainSelect.getJoins())
                tableList.add(join.toString());
            }
            for (String table : tableList) {
                String[] tableNames = table.split(" ");
                if (table.split(" ").length > 1) {
                    String tableName = tableNames[0];
                    String alias = tableNames[tableNames.length-1];
                    Map<String, Integer> orderedSchema = sortByValues(schemas.get(tableName));
                    for (String str : orderedSchema.keySet()) {
                        attributeOrder.add(alias + "." + str.split("\\.")[1]);
                    }
                } else {
                    Map<String, Integer> orderedSchema = sortByValues(schemas.get(table));
                    attributeOrder.addAll(orderedSchema.keySet());
                }
            }
        } else {
            for (Object selectItem : plainSelect.getSelectItems()) {
                attributeOrder.add(selectItem.toString());
            }
        }
    }

    public static HashMap sortByValues(Map map) {
        List list = new LinkedList(map.entrySet());
        // Defined Custom Comparator here
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o1)).getValue())
                        .compareTo(((Map.Entry) (o2)).getValue());
            }
        });
        HashMap sortedHashMap = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            sortedHashMap.put(entry.getKey(), entry.getValue());
        }
        return sortedHashMap;
    }

    /**
     * @return attributeOrder which is the column order of the output tuple
     */
    public List<String> getAttributeOrder() {
        return attributeOrder;
    }

    /**
     * @return singleton instance
     */
    public static Catalog getInstance() {
        if (instance == null) {
            synchronized (Catalog.class) {
                if (instance == null) {
                    instance = new Catalog();//instance will be created at request time
                }
            }
        }
        return instance;
    }

    /**
     * @return current schema
     */
    public Map<String, Integer> getCurrentSchema() {
        return currentSchema;
    }

    /**
     * move the current schema pointer
     *
     * @param alias: change current schema to a new one
     */
    public void updateCurrentSchema(String alias) {
        Map<String, Integer> tempSchema = schemas.get(aliases.get(alias));
        currentSchema = new HashMap<>();
        for (Map.Entry<String, Integer> entry : tempSchema.entrySet()) {
            String newKey = alias + "." + entry.getKey().split("\\.")[1];
            currentSchema.put(newKey, entry.getValue());
        }
    }

    /**
     * move the current schema pointer
     *
     * @param schema: change current schema to a new one
     */
    public void setCurrentSchema(Map<String, Integer> schema) {
        currentSchema = schema;
    }

    /**
     * return the file's path of certain table
     *
     * @param table
     * @return path of data file
     */
    public String getDataPath(String table) {
        return files.get(table);
    }

    public String getIndexPath() {
        return Constants.inputPath + "/db/indexes";
    }

    public String getIndexFile(String schemaName) {
        return getIndexPath() + "/" + schemaName;
    }

    public Map<String, String> getTablePaths() {
        return files;
    }

    public Map<String, Map<String, Integer>> getSchemas() {
        return schemas;
    }

    /**
     * return output path
     *
     * @return output path string
     */
    public String getOutputPath() {
        return outputPath;
    }

    /**
     * return temp path
     *
     * @return temp path string
     */
    public String getTempPath() {
        return Constants.TEMP_PATH;
    }

    /**
     * @return sql queries file path
     */
    public String getSqlQueriesPath() {
        return inputPath;
    }

    /**
     * @return index info file path
     */
    public String getIndexInfoPath(){
        return Constants.inputPath + "/db/index_info.txt";
    }

    /**
     * setter for input path
     *
     * @param inputPath
     */
    public void setInputPath(String inputPath) {
        this.inputPath = inputPath;
    }


    /**
     * setter for output path
     *
     * @param outputPath
     */
    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    /**
     * to set aliases to deal with the occasional failure of sqlparser
     *
     * @param str the str parsed by sqlparser
     */
    public void setAliases(String str) {
        String[] strs = str.split("\\s+");
        if (strs.length == 0 || strs.length == 1) {
            return;
        }

        if (strs.length == 2) {
            aliases.put(strs[1], strs[0]);
        }

        if (strs.length == 3) {
            aliases.put(strs[2], strs[0]);
        }
    }

    /**
     * getColumnNameFromAlias
     *
     * @param alias
     * @return
     */
    public String getTableNameFromAlias(String alias) {
        return aliases.getOrDefault(alias, alias);
    }

    /**
     * get schema for a certain table
     *
     * @param table
     * @return Map with schema
     */
    public Map<String, Integer> getTableSchema(String table) {
        return schemas.get(table);
    }


    /**
     * get index of column
     *
     * @param column
     * @return
     */
    public int getIndexOfColumn(String column) {
        return currentSchema.get(column);
    }

    /**
     * get the join method
     *
     * @return SMJ, TNLj, BNLJ
     */
    public JoinMethod getJoinMethod() {
        return joinMethod;
    }

    /**
     * set the join method
     *
     * @param joinMethod
     */
    public void setJoinMethod(JoinMethod joinMethod) {
        this.joinMethod = joinMethod;
    }

    /**
     * get the sort method
     *
     * @return IN_MEMORY, EXTERNAL
     */
    public SortMethod getSortMethod() {
        return sortMethod;
    }

    /**
     * set the sort method
     *
     * @param sortMethod
     */
    public void setSortMethod(SortMethod sortMethod) {
        this.sortMethod = sortMethod;
    }

    /**
     * get the BNLJ block size
     *
     * @return int
     */
    public int getJoinBlockSize() {
        return this.joinBlockSize;
    }

    /**
     * set the BNLJ block size
     *
     * @param joinBlockSize
     */
    public void setJoinBlockSize(int joinBlockSize) {
        this.joinBlockSize = joinBlockSize;
    }

    /**
     * get the external sort block size
     *
     * @return block size
     */
    public int getSortBlockSize() {
        return this.sortBlockSize;
    }

    /**
     * set the external sort block size
     *
     * @param sortBlockSize
     */
    public void setSortBlockSize(int sortBlockSize) {
        this.sortBlockSize = sortBlockSize;
    }

    public boolean getIndexScan() {
        return this.indexScan;
    }

    public void setIndexScan(Boolean onOff) {
        this.indexScan = onOff;
    }

    Map <String, IndexConfig> indexConfigs = new HashMap<>();

    public IndexConfig setIndexConfig(String config) {
        IndexConfig indexConfig = new IndexConfig(config);
        indexConfigs.put(indexConfig.schemaName, new IndexConfig(config));
        return indexConfig;
    }

    public IndexConfig getIndexConfig(Map<String, Integer> schema) {
        String[] schemaNames = getSchemaNameFromRootSchema(schema);
        if (schemaNames == null) {
            return null;
        }
        for (String schemaName : schemaNames) {
            if (hasIndexConfig(schemaName)) {
                return indexConfigs.get(schemaName);
            }
        }
        return null;
    }

    private String[] getSchemaNameFromRootSchema(Map<String, Integer> schema) {
        String ret[] = new String[schema.size()];
        int i = 0;
        for (Map.Entry<String, Integer> entry : schema.entrySet()) {
            String[] keySplit = entry.getKey().toString().split("\\.+");
            if (keySplit.length < 1) return null;
            ret[i++] = getTableNameFromAlias(keySplit[0]) + "." + keySplit[1];
        }
        return ret;
    }

    public boolean hasIndexConfig(String tableName) {
        return indexConfigs.containsKey(tableName);
    }

    public Map<String, IndexConfig> getIndexConfigs() {
        return indexConfigs;
    }

    /**
     * 
     */
    private boolean buildIndex;
    private boolean evaluateSQL;

    public void setBuildIndex(String arg4) {
        buildIndex = arg4.equals("1");
    }

    public boolean isBuildIndex() {
        return this.buildIndex;
    }

    public void setEvaluateSQL(String arg5) {
        evaluateSQL = arg5.equals("1");
    }

    public boolean isEvaluateSQL() {
        return this.evaluateSQL;
    }

    // Table Stats data
    public String getStatsPath() { return Constants.inputPath + "/db/stats.txt"; }

    private Map<String, String> originTableStats = new HashMap<>();

    public void putStats(String config) {
        String splits[] = config.split("\\s+");
        if (splits.length < 2) {
            throw new IllegalArgumentException("Unexpected Stat.txt format");
        }
        originTableStats.put(splits[0], config);
    }

    public String getStatsConfig(String tableName) {
        if (!originTableStats.containsKey(tableName)) {
            return null;
        }
        return originTableStats.get(tableName);
    }

    public void parserStats() throws IOException {
        File file = new File(Constants.inputPath + "/db/stats.txt");
        BufferedReader br = new BufferedReader(new FileReader(file));
        String config;
        while ((config = br.readLine()) != null) {
            putStats(config);
        }
        br.close();
    }

    private Map<String, TableHistogram> originHistograms = new HashMap<>();

    public void setOriginHistograms(String key, TableHistogram tableHistogram) {
        originHistograms.put(key, tableHistogram);
    }

    public Histogram getHsitogram (String columnName) {
        String [] splits = columnName.split("\\.");
        String tableName = splits[0];
        Histogram histogram = originHistograms.get(tableName).getHistogram(columnName);
        return histogram;
    }
}
