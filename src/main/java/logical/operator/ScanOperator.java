package logical.operator;

import java.util.Map;

import com.sql.interpreter.PhysicalPlanBuilder;
import net.sf.jsqlparser.statement.select.PlainSelect;

import util.Catalog;
import io.BinaryTableReader;


/**
 * PhysicalScanOperator
 * Read the table from disk and fetch a tuple
 */
public class ScanOperator extends Operator{
    private Operator op;
    private BinaryTableReader binaryTableReader;
    private Map<String, Integer> schema;

    /**
     * 
     * @param plainSelect is the statement of sql
     * @param tableIndex is the index of the table in FROM section, start by 0
     */
    public ScanOperator(PlainSelect plainSelect, int tableIndex){
        this.op = null;
        
        String item;
        if(tableIndex == 0){
            item = plainSelect.getFromItem().toString();
        }
        else{
            item = plainSelect.getJoins().get(tableIndex-1).toString();
        }

        String[] strs = item.split("\\s+");
        if(strs.length < 0){
            this.binaryTableReader = null;
            return;
        }
        String tableName = strs[0];
        String aliasName = strs[strs.length - 1];

        Catalog.getInstance().setAliases(item);
        Catalog.getInstance().updateCurrentSchema(aliasName);

        this.schema = Catalog.getInstance().getCurrentSchema();
        binaryTableReader = new BinaryTableReader(tableName);
    }

    /**
     * get the schema
     */
    @Override
    public Map<String, Integer> getSchema(){
        return this.schema;
    }

    public BinaryTableReader getBinaryTableReader() {
        return binaryTableReader;
    }

    /**
     * method to get children
     */
    @Override
    public Operator[] getChildren(){
        if(this.op == null){
            return null;
        }
        else{
            return new Operator[] {this.op};
        }
    }

    @Override
    public void accept(PhysicalPlanBuilder visitor) {
        visitor.visit(this);
    }
}