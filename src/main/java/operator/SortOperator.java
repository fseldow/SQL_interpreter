package operator;

import net.sf.jsqlparser.statement.select.PlainSelect;
import util.Catalog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * SortOperator
 * created by Yufu Mo ym445
 */
public class SortOperator extends Operator{
    private List<Tuple> tupleList;
    private final PlainSelect plainSelect;
    private int currentIndex;
    private Map<String, Integer> schema;

    public SortOperator(Operator operator, PlainSelect plainSelect) {
        tupleList = new ArrayList<Tuple>();
        this.plainSelect = plainSelect;
        this.schema = operator.getSchema();

        // initialize the list
        Tuple tuple = operator.getNextTuple();
        while(tuple != null) {
            tupleList.add(tuple);
            tuple = operator.getNextTuple();
        }

        Collections.sort(tupleList, new TupleComparator());
        operator.reset();
    }

    /**
     * get the next tuple of the operator.
     */
    @Override
    public Tuple getNextTuple() {
        // TODO Auto-generated method stub
        Tuple tuple = null;
        if (currentIndex < tupleList.size()) {
            tuple = tupleList.get(currentIndex);
        }
        currentIndex++;
        return tuple;
    }

    /**
     * reset the operator.
     */
    @Override
    public void reset() {
        // TODO Auto-generated method stub
        currentIndex = 0;
    }

    /**
     * get the schema
     */
    @Override
    public Map<String, Integer> getSchema(){
        return this.schema;
    }

    /**
     * for debugging, get all the tuples at once and put them in a file.
     * @param i the index of the output file.
     */
    @Override
    public void dump(int i) {
        // TODO Auto-generated method stub
        String path = Catalog.getInstance().getOutputPath();
        BufferedWriter output;
        try{
            File file = new File(path + i);
            StringBuilder sb = new StringBuilder();
            output = new BufferedWriter(new FileWriter(file));
            Tuple tuple = getNextTuple();

            while(tuple != null){
                sb.append(tuple.toString());
                sb.append("\n");
                System.out.println(tuple);
                tuple = getNextTuple();
            }
            output.write(sb.toString());
            output.close();
        }catch(IOException e){
            System.out.println("An exception occurs!");
        }
        reset();
    }


    /**
     * comparator to sort tuples
     */
    class TupleComparator implements Comparator<Tuple> {

        List<Integer> order = plainSelect.getOrderByElements();

        @Override
        public int compare(Tuple t1, Tuple t2) {
            // TODO Auto-generated method stub
            // sort tuples from the order from sql query.
            for (int i = 0; i < order.size(); i++) {
                String str = order.get(i).toString();
                int index = Catalog.getInstance().getIndexOfColumn(str);
                if (t1.getDataAt(index) > t2.getDataAt(index)) {
                    return 1;
                }
                if (t1.getDataAt(index) < t2.getDataAt(index)) {
                    return -1;
                }
            }

            // for tie breaker
            // sort tuples by the order of columns.
            for (int i = 0; i < Catalog.getInstance().getCurrentSchema().size(); i++){
                if (t1.getDataAt(i) > t2.getDataAt(i)) {
                    return 1;
                }
                if (t1.getDataAt(i) < t2.getDataAt(i)) {
                    return -1;
                }
            }
            return 0;
        }
    }
}
