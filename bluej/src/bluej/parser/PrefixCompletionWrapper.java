/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015 Michael Kölling and John Rosenberg 
 
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
package bluej.parser;

import java.util.List;
import java.util.concurrent.Executor;

import bluej.parser.AssistContent.JavadocCallback;
import bluej.stride.framedjava.ast.AccessPermission;
import threadchecker.OnThread;
import threadchecker.Tag;

@OnThread(Tag.FXPlatform)
public class PrefixCompletionWrapper extends AssistContent
{
    private final String prefix; 
    private final AssistContent wrapped;

    public PrefixCompletionWrapper(AssistContent wrapped, String prefix)
    {
        this.prefix = prefix;
        this.wrapped = wrapped;
    }

    @OnThread(Tag.Any)
    public String getName()
    {
        return prefix + wrapped.getName();
    }

    public List<ParamInfo> getParams()
    {
        return wrapped.getParams();
    }

    public String getType()
    {
        return wrapped.getType();
    }

    public String getDeclaringClass()
    {
        return wrapped.getDeclaringClass();
    }

    public CompletionKind getKind()
    {
        return wrapped.getKind();
    }

    public String getJavadoc()
    {
        return wrapped.getJavadoc();
    }
    
    @Override
    public boolean getJavadocAsync(JavadocCallback callback, Executor executor)
    {
        return wrapped.getJavadocAsync(callback, executor);
    }

    @Override
    public Access getAccessPermission()
    {
        return wrapped.getAccessPermission();
    }
}
