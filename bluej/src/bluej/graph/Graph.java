package bluej.graph;

import java.awt.*;
import java.util.Iterator;
import java.awt.geom.*;

/**
 * General graph
 *
 * @author  Michael Cahill
 * @author  Michael Kolling
 * @version $Id: Graph.java 2787 2004-07-12 14:12:42Z mik $
 */
public abstract class Graph
{
    private static final int RIGHT_PLACEMENT_MIN = 300;
    private static final int WHITESPACE_SIZE = 10;
    
    public abstract Iterator getVertices();
    public abstract Iterator getEdges();
    
    
    public Dimension getMinimumSize()
    {
        int minWidth = 1;
        int minHeight = 1;

        for(Iterator it = getVertices(); it.hasNext(); ) {
            Vertex v = (Vertex)it.next();

            if(v.getX() + v.getWidth() > minWidth)
                minWidth = v.getX() + v.getWidth();
            if(v.getY() + v.getHeight() > minHeight)
                minHeight = v.getY() + v.getHeight();
        }

        return new Dimension(minWidth + 20, minHeight + 20);  // add some space for looks
    }

    
    public void findSpaceForVertex(Vertex t)
    {
        Area a = new Area();

        for(Iterator it = getVertices(); it.hasNext(); ) {
            Vertex vertex = (Vertex)it.next();

            // lets discount the vertex we are adding from the space
            // calculations
            if (vertex != t) {
                Rectangle vr = new Rectangle(vertex.getX(), vertex.getY(),
                                                vertex.getWidth(), vertex.getHeight());
                a.add(new Area(vr));
            }
        }

        Dimension min = getMinimumSize();

        if (RIGHT_PLACEMENT_MIN > min.width)
            min.width = RIGHT_PLACEMENT_MIN;

        Rectangle targetRect = new Rectangle(t.getWidth() + WHITESPACE_SIZE*2,
                                                t.getHeight() + WHITESPACE_SIZE*2);

        for(int y=0; y<(2*min.height); y+=10) {
            for(int x=0; x<(min.width-t.getWidth()-2*WHITESPACE_SIZE); x+=10) {
                targetRect.setLocation(x,y);
                if (!a.intersects(targetRect)) {
                    t.setPos(x+10,y+10);
                    return;
                }
            }
        }

        t.setPos(10,min.height+10);
    }
    
    
    /**
     * Finds the graphElement that covers the coordinate x,y. If no element is
     * found, null is returned. If a Vertex and an Edge both covers x, y the
     * Vertex will be returned.
     * 
     * @param x
     *            the x coordinate
     * @param y
     *            the x coordinate
     * @return GraphElement
     */
    public SelectableGraphElement findGraphElement(int x, int y)
    {
        SelectableGraphElement element = findVertex(x, y);

        if (element == null) {
            element = findEdge(x, y);
        }
        return element;
    }

    
    /**
     * Finds the Edge that covers the coordinate x,y. If no edge is found, null
     * is returned.
     * 
     * @param x     the x coordinate
     * @param y     the y coordinate
     * @return  an edge at that position, or null
     */
    private Edge findEdge(int x, int y)
    {
        GraphElement element = null;
        for (Iterator it = getEdges(); it.hasNext();) {
            element = (GraphElement) it.next();
            if (element.contains(x, y)) {
                return (Edge) element;
            }
        }
        return null;
    }

    /**
     * Finds the Vertex that covers the coordinate x,y. If no vertex is found,
     * null is returned.
     * 
     * @param x     the x coordinate
     * @param y     the y coordinate
     * @return  a vertex at that position, or null
     */
    private Vertex findVertex(int x, int y)
    {
        GraphElement element = null;
        GraphElement topElement = null;

        //Try to find a vertex containing the point
        // Rather than breaking when we find the vertex we keep searching
        // which will therefore find the LAST vertex containing the point
        // This turns out to be the vertex which is rendered at the front
        for (Iterator it = getVertices(); it.hasNext();) {
            element = (GraphElement) it.next();
            if (element.contains(x, y)) {
                topElement = element;
            }
        }
        return (Vertex) topElement;
    }
}
