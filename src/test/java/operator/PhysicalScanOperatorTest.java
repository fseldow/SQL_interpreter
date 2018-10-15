package operator;

import logical.operator.ScanOperator;
import org.junit.Test;

import junit.framework.Assert;

import java.io.*;
import java.util.ArrayList;

import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import util.Constants;
import model.Tuple;

public class PhysicalScanOperatorTest {

    @Test
    public void testReadFile() throws Exception{
        String statement = "SELECT * FROM Sailors, Reserves WHERE Sailors.A = Reserves.G;";
        CCJSqlParserManager parserManager = new CCJSqlParserManager();
        PlainSelect plainSelect = (PlainSelect) ((Select) parserManager.parse(new StringReader(statement))).getSelectBody();
        ScanOperator logScanOp = new ScanOperator(plainSelect, 0);
        PhysicalOperator physScanOp = new PhysicalScanOperator(logScanOp);

        // read expected result from disk
        ArrayList<String> expectedResult = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(Constants.DATA_PATH + "Sailors_humanreadable"));
        String line;
        while((line = br.readLine())!=null){
            expectedResult.add(line);
        }
        br.close();
        // get scanOperator result
        ArrayList<String> outputResult = new ArrayList<>();
        Tuple tuple;
        while((tuple = physScanOp.getNextTuple()) != null){
            outputResult.add(tuple.toString());
        }
        Assert.assertEquals(expectedResult, outputResult);
    }
}