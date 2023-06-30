package muliti.objective.opt.submit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

@SuppressWarnings(
{
		"unchecked", "rawtypes", "restriction"
})
public class Nsga2Graph extends Application
{
	static String                   screenTitle;
	static String                   xLabel;
	static String                   yLabel;
	static String                   graphTitle;
	static Map<String, List<Point>> pointMap;
	int currentChangeCount=-1;
	int previousRunCount=-1;
	int runCount=0;
			int maxRunCount=10;
	static List<XYChart.Series<Number, Number>> seriesList = new ArrayList<XYChart.Series<Number, Number>>();
	static boolean isClosed=false;

	public static void setGraphData(GraphData gd)
	{
		Nsga2Graph.screenTitle=gd.getGraphTitle();
		Nsga2Graph.xLabel=gd.getXLabel();
		Nsga2Graph.yLabel=gd.getYLabel();
		Nsga2Graph.graphTitle=gd.getGraphTitle();
		Nsga2Graph.pointMap=gd.getPointMap();
		
	}

	public static void getSeries()
	{

		for (String chartName : pointMap.keySet())
		{
			List<Point> pointList = pointMap.get(chartName);

			XYChart.Series<Number, Number> series1 = new XYChart.Series<Number, Number>();
			series1.setName(chartName);
			for (Point p : pointList)
			{

				series1.getData().add(new XYChart.Data(p.getX(), p.getY()));
			}
			Nsga2Graph.seriesList.add(series1);
		}
	}
	public void fetchData()
	{
		for(int i=0;i<2;i++)
		{
		GraphData gd =new Nsga2Impl().fetchDataForJavaFx();
		setGraphData(gd);
		getSeries();
		this.currentChangeCount=gd.getChangeCount();

		}
	}

	@Override
	public void start(Stage stage)
	{
		stage.setTitle(screenTitle);
		final NumberAxis                   xAxis = new NumberAxis(0, 800, 200);
		final NumberAxis                   yAxis = new NumberAxis(-100, 800, 100);
		final ScatterChart<Number, Number> sc    = new ScatterChart<Number, Number>(xAxis, yAxis);
		xAxis.setLabel(xLabel);
		yAxis.setLabel(yLabel);
		sc.setTitle(graphTitle);
   		sc.getData().addAll(seriesList);

		Scene scene = new Scene(sc, 500, 400);
		stage.setScene(scene);
		stage.show();

		Set<Node> nodes = sc.lookupAll(".series" + 0);
		for (Node n : nodes)
		{
			n.setStyle("-fx-background-color: #860061,white;\n" + "    -fx-background-insets: 0, 2;\n"
					+ "    -fx-background-radius: 5px;\n" + "    -fx-padding: 5px;");
		}
		
callThread(sc);

	}

public void callThread(ScatterChart<Number, Number> sc )
{
	
	 Thread thread = new Thread(new Runnable() {

           @Override
           public void run() {
               Runnable updater = new Runnable() {

                   @Override
                   public void run() {
//                   	
//                   	if(isClosed==false)
//                   	{
//                   		isClosed=true;
//                       fetchData();
//                       previousRunCount++;
//                   	
//                   	sc.getData().removeAll(seriesList);
//                   	if(sc.getData().size()==0)
//               		sc.getData().addAll(seriesList);
//               		isClosed=false;
//                   	}
//                   	runCount++;
//                       System.out.println(isClosed+" = isclosed "+runCount+" =runcount , CurrentChangeCount="+currentChangeCount+" previousChangCount="+previousRunCount);
//
//                   	
               		

                   }
               };

               while (true) {
                   try {
                       Thread.sleep(500);
                   } catch (InterruptedException ex) {
                   }

                   // UI update is run on the Application thread
                   Platform.runLater(updater);
               }
           }

       });
       // don't let thread prevent JVM shutdown
       thread.setDaemon(true);
       thread.start();
	
}
	public static void main(String[] args)
	{
////		for(int i=0;i<10;i++)
//		{
		boolean isDebug=true;
		int sampleSize=100;
		int programRunCountMax=1;
		GraphData gd =new Nsga2Impl().iterateProgram(isDebug, programRunCountMax, sampleSize);
				
		setGraphData(gd);
		getSeries();
//
		launch(args);
//		}
	}
}
