/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.parser.nodes;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.text.Document;

import bluej.editor.moe.MoeSyntaxDocument;
import bluej.parser.DocumentReader;
import bluej.parser.EditorParser;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.PackageOrClass;
import bluej.parser.nodes.NodeTree.NodeAndPosition;


/**
 * A parsed compilation unit node.
 * 
 * @author Davin McCall
 */
public class ParsedCUNode extends ParentParsedNode
{
    private Document document;
    private EntityResolver parentResolver;

    private List<NodeStructureListener> listeners = new ArrayList<NodeStructureListener>();
    
    /**
     * Construct a parsed node for the given document.
     */
    public ParsedCUNode(Document document)
    {
        this.document = document;
    }
    
    /**
     * Set the entity resolver used to resolve symbols.
     */
    public void setParentResolver(EntityResolver parentResolver)
    {
        this.parentResolver = parentResolver;
    }

    /**
     * Add a structure listener to this compilation unit.
     */
    public void addListener(NodeStructureListener listener)
    {
        listeners.add(listener);
    }
    
    /**
     * Remove a structure listener from this compilation unit.
     */
    public void removeListener(NodeStructureListener listener)
    {
        listeners.remove(listener);
    }
    
    /**
     * Overridden getSize() which returns the document size.
     * 
     * @see bluej.parser.nodes.ParsedNode#getSize()
     */
    public int getSize()
    {
        return document.getLength();
    }

    public int lineColToPosition(int line, int col)
    {
        return document.getDefaultRootElement().getElement(line - 1).getStartOffset() + col - 1;
    }
	
    /**
     * Reparse this node from the specified offset.
     */
    protected void reparseNode(Document document, int nodePos, int offset)
    {
        doReparse(document, 0, offset);
    }
    
    protected void doReparse(Document document, int nodePos, int pos)
    {
        clearNode(this);
        
        Reader r = new DocumentReader(document);
        EditorParser parser = new EditorParser(r);
        parser.parseCU(this);
	    
        ((MoeSyntaxDocument) document).documentChanged();
    }
    
    /**
     * Remove all subnodes from the given node.
     */
    private void clearNode(ParsedNode node)
    {
        Iterator<NodeAndPosition> i = node.getNodeTree().iterator();
        while (i.hasNext()) {
            NodeAndPosition nap = i.next();
            clearNode(nap.getNode());
            for (Iterator<NodeStructureListener> j = listeners.iterator(); j.hasNext(); ) {
                j.next().nodeRemoved(nap);
            }
        }
        node.getNodeTree().clear();
    }
    
    @Override
    public PackageOrClass resolvePackageOrClass(String name, String querySource)
    {
        PackageOrClass poc = super.resolvePackageOrClass(name, querySource);
        if (poc == null && parentResolver != null) {
            return parentResolver.resolvePackageOrClass(name, querySource);
        }
        return poc;
    }
}
