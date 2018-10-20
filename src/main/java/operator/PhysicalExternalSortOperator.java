package operator;

import logical.operator.SortOperator;
import model.Tuple;
import io.TupleReader;
import util.Catalog;
import util.Constants;
import io.BinaryTableReader;
import io.TempWriter;
import net.sf.jsqlparser.statement.select.PlainSelect;

import java.util.*;

public class PhysicalExternalSortOperator extends PhysicalSortOperator{
    private List<TupleReader> buffer;
    private final String id;
    private BinaryTableReader outputBuffer;
    int blockSize;

    public PhysicalExternalSortOperator(PhysicalOperator operator, PlainSelect plainSelect){
        super(operator, plainSelect);
        this.blockSize = Catalog.getInstance().getSortBlockSize();
        buffer = new ArrayList<>(blockSize - 1);
        id = UUID.randomUUID().toString().substring(0, 8);
        outputBuffer = new BinaryTableReader(Catalog.getInstance().getOutputPath(), id);
        firstRun();
    } 

    public PhysicalExternalSortOperator(SortOperator logSortOp, Deque<PhysicalOperator> physChildren){
        super(logSortOp, physChildren);
        this.blockSize = Catalog.getInstance().getSortBlockSize();
        buffer = new ArrayList<>(blockSize - 1);
        id = UUID.randomUUID().toString().substring(0, 8);
        outputBuffer = new BinaryTableReader(Catalog.getInstance().getOutputPath(), id);
        firstRun();
    }

    private void firstRun(){
        try{
            int index = 0;
            while(true){
                int tupleCount = blockSize
                                * ((Constants.PAGE_SIZE - 2 * Constants.INT_SIZE) / (schema.size() * Constants.INT_SIZE));
                List<Tuple> tupleList = new ArrayList<>();
                for(int i =0 ;i < tupleCount; ++i){
                    Tuple tuple = physChild.getNextTuple();
                    if(tuple == null) break;
                    tupleList.add(tuple);
                }
                if(tupleList.size() == 0){
                    break;
                }
                TempWriter tempWriter = new TempWriter("run1_" + index);
                index ++;
                tempWriter.writeTupleList(tupleList);
            }
        }catch(Exception e){
            e.printStackTrace();
        }

    }
}