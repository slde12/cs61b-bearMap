import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.*;


/**
 * Graph for storing all of the intersection (vertex) and road (edge) information.
 * Uses your GraphBuildingHandler to convert the XML files into a graph. Your
 * code must include the vertices, adjacent, distance, closest, lat, and lon
 * methods. You'll also need to include instance variables and methods for
 * modifying the graph (e.g. addNode and addEdge).
 *
 * @author Alan Yao, Josh Hug
 */
public class GraphDB {
    /** Your instance variables for storing the graph. You should consider
     * creating helper classes, e.g. Node, Edge, etc. */

    /**
     * Example constructor shows how to create and start an XML parser.
     * You do not need to modify this constructor, but you're welcome to do so.
     * @param dbPath Path to the XML file to be parsed.
     */

    private final Map<Long, Node> nodes = new LinkedHashMap<>();
    private final Map<Long, Way> ways = new LinkedHashMap<>();
    private final Map<Long, ArrayList<Long>> paths = new LinkedHashMap<>();
    private final Map<String, Long> pathId = new LinkedHashMap<>();
    private final Trie root = new Trie();
    private Map<Long, Node> allNodes;

    public GraphDB(String dbPath) {
        try {
            File inputFile = new File(dbPath);
            FileInputStream inputStream = new FileInputStream(inputFile);
            // GZIPInputStream stream = new GZIPInputStream(inputStream);

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            GraphBuildingHandler gbh = new GraphBuildingHandler(this);
            saxParser.parse(inputStream, gbh);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        clean();
    }

    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     * @param s Input string.
     * @return Cleaned string.
     */
    static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /**
     *  Remove nodes with no connections from the graph.
     *  While this does not guarantee that any two nodes in the remaining graph are connected,
     *  we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        allNodes = new LinkedHashMap<>(nodes);
        Iterable<Long> vs = vertices();
        for(long v : vs)
            if( !nodes.get(v).connected )
                nodes.remove(v);
    }

    /**
     * Returns an iterable of all vertex IDs in the graph.
     * @return An iterable of id's of all vertices in the graph.
     */
    Iterable<Long> vertices() { return new ArrayList<Long>(nodes.keySet()); }

    Iterable<Long> ways() { return new ArrayList<Long>(ways.keySet()); }

    /**
     * Returns ids of all vertices adjacent to v.
     * @param v The id of the vertex we are looking adjacent to.
     * @return An iterable of the ids of the neighbors of v.
     */
    Iterable<Long> adjacent(long v) { return new ArrayList<Long>(paths.get(v)); }

    /**
     * Returns the great-circle distance between vertices v and w in miles.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The great-circle distance between the two locations from the graph.
     */
    double distance(long v, long w) {
        return distance(lon(v), lat(v), lon(w), lat(w));
    }

    static double distance(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double dphi = Math.toRadians(latW - latV);
        double dlambda = Math.toRadians(lonW - lonV);

        double a = Math.sin(dphi / 2.0) * Math.sin(dphi / 2.0);
        a += Math.cos(phi1) * Math.cos(phi2) * Math.sin(dlambda / 2.0) * Math.sin(dlambda / 2.0);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 3963 * c;
    }

    /**
     * Returns the initial bearing (angle) between vertices v and w in degrees.
     * The initial bearing is the angle that, if followed in a straight line
     * along a great-circle arc from the starting point, would take you to the
     * end point.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The initial bearing between the vertices.
     */
    double bearing(long v, long w) {
        return bearing(lon(v), lat(v), lon(w), lat(w));
    }

    static double bearing(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double lambda1 = Math.toRadians(lonV);
        double lambda2 = Math.toRadians(lonW);

        double y = Math.sin(lambda2 - lambda1) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2);
        x -= Math.sin(phi1) * Math.cos(phi2) * Math.cos(lambda2 - lambda1);
        return Math.toDegrees(Math.atan2(y, x));
    }

    /**
     * Returns the vertex closest to the given longitude and latitude.
     * @param lon The target longitude.
     * @param lat The target latitude.
     * @return The id of the node in the graph closest to the target.
     */
    long closest(double lon, double lat) {
        double minimum = Integer.MAX_VALUE;
        long res = 0;
        for(long v : vertices()){
            double dist = distance(lon(v), lat(v), lon, lat);
            if(dist < minimum){
                minimum = dist;
                res = v;
            }
        }
        return res;
    }

    /**
     * Gets the longitude of a vertex.
     * @param v The id of the vertex.
     * @return The longitude of the vertex.
     */
    double lon(long v) { return nodes.get(v).longitude; }

    /**
     * Gets the latitude of a vertex.
     * @param v The id of the vertex.
     * @return The latitude of the vertex.
     */
    double lat(long v) { return nodes.get(v).latitude; }

    static class Node{
        Long id;
        double longitude;
        double latitude;
        boolean connected;
        Map<String, String> extraInfo;

        Node(String id, double longitude, double latitude){
            this.id = Long.parseLong(id);
            this.longitude = longitude;
            this.latitude = latitude;
            this.extraInfo = new HashMap<>();
            this.connected = false;
        }

        void setNode(String k, String v){
            this.extraInfo.put(k, v);
        }
    }

    void addNode(Node n){ this.nodes.put(n.id, n); }

    void deleteNode(Node n){ this.nodes.remove(n.id); }

    void deleteNode(String id){ this.nodes.remove(Long.parseLong(id)); }

    void deleteNode(Long id){ this.nodes.remove(id); }

    Node getNode(Long id){ return this.nodes.get(id); }

    static class Way{
        Long id;
        Map<String, String> extraInfo;
        ArrayList<Long> edges;//nodes on this way
        Way(String id){
            this.id = Long.parseLong(id);
            this.extraInfo = new HashMap<>();
            this.edges = new ArrayList<>();
        }

        void addNode(String currentId){ this.edges.add(Long.parseLong(currentId)); }

        void addNode(long currentId){ this.edges.add(currentId); }

        void setEdge(String k, String v){ this.extraInfo.put(k, v); }
    }

    void addWay(Way w){ this.ways.put(w.id, w); }

    Way getWay(long id){ return this.ways.get(id); }

    void buildPath(Way w){
        if(w.extraInfo.get("oneway") != null && w.extraInfo.get("oneway").equals("yes")){
            for(int i = 1; i < w.edges.size(); i++){
                long begin = w.edges.get(i-1);
                long end = w.edges.get(i);
                if(this.paths.get(begin) == null)
                    this.paths.put(begin, new ArrayList<>());
                this.paths.get(begin).add(end);
                this.nodes.get(begin).connected = true;
                this.nodes.get(end).connected = true;
                String fromTo = String.valueOf(begin) + "to" + String.valueOf(end);
                this.pathId.put(fromTo, w.id);
            }
        }else{
            for(int i = 1; i < w.edges.size(); i++){
                long begin = w.edges.get(i-1);
                long end = w.edges.get(i);
                if(this.paths.get(begin) == null)
                    this.paths.put(begin, new ArrayList<>());
                if(this.paths.get(end) == null)
                    this.paths.put(end, new ArrayList<>());
                this.paths.get(begin).add(end);
                this.paths.get(end).add(begin);
                this.nodes.get(begin).connected = true;
                this.nodes.get(end).connected = true;
                String fromTo = String.valueOf(begin) + "to" + String.valueOf(end);
                this.pathId.put(fromTo, w.id);
                fromTo = String.valueOf(end) + "to" + String.valueOf(begin);
                this.pathId.put(fromTo, w.id);
            }
        }
    }

    long getPathId(Long from, Long to){
        String fromTo = String.valueOf(from) + "to" + String.valueOf(to);
        return this.pathId.get(fromTo);
    }

    String getWayName(long id){
        return this.ways.get(id).extraInfo.get("name");
    }

    static class Trie{
        boolean isEnd;
        Trie[] children;
        String value;
        LinkedList<Long> ids;
        final int size = 27;
        Trie(){
            this.children = new Trie[size];
            Arrays.fill(this.children, null);
            this.isEnd = false;
        }

        void insert(String word, long id){
            Trie node = this;
            //System.out.println(word);
            String cleanedWord = cleanString(word);
            //System.out.println(cleanedWord);
            for(char c : cleanedWord.toCharArray()){
                int idx = (c - 'a') >= 0 ? (c - 'a') : 26;
                if(node.children[idx] == null){
                    node.children[idx] = new Trie();
                }
                node = node.children[idx];
            }
            node.isEnd = true;
            node.value = word;
            if(node.ids == null)
                node.ids = new LinkedList<>();
            node.ids.add(id);
        }

        LinkedList<String> searchPrefix(String prefix){
            LinkedList<String> res = new LinkedList<>();
            Trie node = this;
            int cnt = 0;
            String cleanedWord = cleanString(prefix);
            for(char c : cleanedWord.toCharArray()){
                int idx = (c - 'a') >= 0 ? (c - 'a') : 26;
                if(node.children[idx] == null) break;
                node = node.children[idx];
                cnt++;
            }
            if(cnt < cleanedWord.length()) return res;
            node.search(res);
            return res;
        }

        LinkedList<Long> searchIdByName(String name){
            String cleanedName = cleanString(name);
            LinkedList<Long> res = new LinkedList<>();
            Trie node = this;
            int cnt = 0;
            for(char c : cleanedName.toCharArray()){
                int idx = (c - 'a') >= 0 ? (c - 'a') : 26;
                if(node.children[idx] == null) break;
                node = node.children[idx];
                cnt++;
            }
            if(cnt < cleanedName.length()) return res;
            return node.ids;
        }

        void search(LinkedList<String> res){
            if(this.isEnd)
                res.add(this.value);
            for(int i = 0; i < size; i++){
                if(this.children[i] != null){
                    this.children[i].search(res);
                }
            }
        }
    }

    void insertTrie(String word, long id){ this.root.insert(word, id); }


    LinkedList<String> searchTriePrefix(String prefix){ return this.root.searchPrefix(prefix); }

    LinkedList<Map<String, Object>> getLocations(String locationName){
        LinkedList<Map<String, Object>> res = new LinkedList<>();
        LinkedList<Long> ids = this.root.searchIdByName(locationName);
        for(long id : ids){
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("lat", this.allNodes.get(id).latitude);
            item.put("lon", this.allNodes.get(id).longitude);
            item.put("name", this.allNodes.get(id).extraInfo.get("name"));
            item.put("id", id);
            res.add(item);
        }
        return res;
    }
}
