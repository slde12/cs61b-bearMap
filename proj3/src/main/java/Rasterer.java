import java.util.HashMap;
import java.util.Map;

/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 */
public class Rasterer {
    public static final double[] LONDPP = {0.00034332275390625, 0.000171661376953125, 0.0000858306884765625,
            0.00004291534423828125, 0.000021457672119140625, 0.000010728836059570312, 0.000005364418029785156,
            0.000002682209014892578};
    public static final double[] WIDTH_UNIT = {0.087890625, 0.0439453125, 0.02197265625, 0.010986328125,
            0.0054931640625, 0.00274658203125, 0.001373291015625, 0.0006866455078125};
    public static final double[] HEIGHT_UNIT = {0.06939311371679224, 0.03469655685839257, 0.017348278429196284,
            0.008674139214598142, 0.004337069607295518, 0.0021685348036442065, 0.0010842674018221032,
            0.0005421337009110516};
    public static final double ROOT_ULLAT = 37.892195547244356, ROOT_ULLON = -122.2998046875,
            ROOT_LRLAT = 37.82280243352756, ROOT_LRLON = -122.2119140625;
    public static final int BASE_PIXEL = 256;
    public static final int[] SIZE = {1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024};
    public Rasterer() {
    }

    /**
     * Takes a user query and finds the grid of images that best matches the query. These
     * images will be combined into one big image (rastered) by the front end. <br>
     *
     *     The grid of images must obey the following properties, where image in the
     *     grid is referred to as a "tile".
     *     <ul>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         (LonDPP) possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *     </ul>
     *
     * @param params Map of the HTTP GET request's query parameters - the query box and
     *               the user viewport width and height.
     *
     * @return A map of results for the front end as specified: <br>
     * "render_grid"   : String[][], the files to display. <br>
     * "raster_ul_lon" : Number, the bounding upper left longitude of the rastered image. <br>
     * "raster_ul_lat" : Number, the bounding upper left latitude of the rastered image. <br>
     * "raster_lr_lon" : Number, the bounding lower right longitude of the rastered image. <br>
     * "raster_lr_lat" : Number, the bounding lower right latitude of the rastered image. <br>
     * "depth"         : Number, the depth of the nodes of the rastered image <br>
     * "query_success" : Boolean, whether the query was able to successfully complete; don't
     *                    forget to set this to true on success! <br>
     */
    public Map<String, Object> getMapRaster(Map<String, Double> params) {
        //System.out.println(params);
        Map<String, Object> results = new HashMap<>();
        double lrlon = params.get("lrlon");
        double ullon = params.get("ullon");
        double lrlat = params.get("lrlat");
        double ullat = params.get("ullat");
        double width = params.get("w");
        double height = params.get("h");

        double requiredLonDPP = (lrlon - ullon) / width;
        int depth = 7;
        for(int i = 0; i < 7; i++){
            if(LONDPP[i] < requiredLonDPP){
                depth = i;
                break;
            }
        }

        int ulx = Math.max(0, (int)((params.get("ullon") - ROOT_ULLON) / WIDTH_UNIT[depth]));
        int uly = Math.max(0, (int)((ROOT_ULLAT - params.get("ullat")) / HEIGHT_UNIT[depth]));
        int lrx = Math.min((int)((ROOT_LRLON - ROOT_ULLON) / WIDTH_UNIT[depth]) - 1,
                (int)((params.get("lrlon") - ROOT_ULLON) / WIDTH_UNIT[depth]));
        int lry = Math.min((int)((ROOT_ULLAT - ROOT_LRLAT) / HEIGHT_UNIT[depth]) - 1,
                (int)((ROOT_ULLAT - params.get("lrlat")) / HEIGHT_UNIT[depth]));

        String[][] render_grid = new String[lry - uly + 1][lrx - ulx + 1];
        for(int y = 0; y < render_grid.length; y++){
            for(int x = 0; x < render_grid[0].length; x++){
                String png = "d" + String.valueOf(depth) + "_x" + String.valueOf(ulx+x) + "_y" + String.valueOf(uly+y) + ".png";
                //System.out.println(png);
                render_grid[y][x] = png;
            }
        }
        double raster_ul_lon = ROOT_ULLON + ulx * WIDTH_UNIT[depth];
        double raster_ul_lat = ROOT_ULLAT - uly * HEIGHT_UNIT[depth];
        double raster_lr_lon = ROOT_ULLON + (lrx + 1) * WIDTH_UNIT[depth];
        double raster_lr_lat = ROOT_ULLAT - (lry + 1) * HEIGHT_UNIT[depth];
        boolean query_success = true;
        if(lrlon <= ROOT_ULLON || ullon >= ROOT_LRLON || lrlat >= ROOT_ULLAT || ullat <= ROOT_LRLAT)
            query_success = false;

        results.put("render_grid", render_grid);
        results.put("raster_ul_lon", raster_ul_lon);
        results.put("raster_ul_lat", raster_ul_lat);
        results.put("raster_lr_lon", raster_lr_lon);
        results.put("raster_lr_lat", raster_lr_lat);
        results.put("depth", depth);
        results.put("query_success", query_success);
        //System.out.println("Since you haven't implemented getMapRaster, nothing is displayed in "
        //                    + "your browser.");
        return results;
    }
}
