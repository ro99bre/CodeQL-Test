<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.filters.*" %>
<%@ page import="java.util.*" %>
<%@ page import="com.ecyrd.jspwiki.htmltowiki.HtmlStringToWikiTranslator" %>
<%@ page import="com.ecyrd.jspwiki.ui.EditorManager" %>
<%@ page import="com.ecyrd.jspwiki.workflow.DecisionRequiredException" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<%!
    Logger log = Logger.getLogger("JSPWiki");

    String findParam( PageContext ctx, String key )
    {
        ServletRequest req = ctx.getRequest();

        String val = req.getParameter( key );

        if( val == null )
        {
            val = (String)ctx.findAttribute( key );
        }

        return val;
    }
%>

<%
    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
    // Create wiki context and check for authorization
    WikiContext wikiContext = wiki.createContext( request, WikiContext.EDIT );
    if(!wikiContext.hasAccess( response )) return;
    String pagereq = wikiContext.getName();

    WikiSession wikiSession = wikiContext.getWikiSession();
    String user = wikiSession.getUserPrincipal().getName();
    String action  = request.getParameter("action");
    String ok      = request.getParameter("ok");
    String preview = request.getParameter("preview");
    String cancel  = request.getParameter("cancel");
    String append  = request.getParameter("append");
    String edit    = request.getParameter("edit");
    String author  = findParam( pageContext, "author" );
    String changenote = findParam( pageContext, "changenote" );
    String text    = EditorManager.getEditedText( pageContext );
    String link    = findParam( pageContext, "link");
    String edittime = findParam( pageContext, "edittime");
    String captcha = (String)session.getAttribute("captcha");

    if ( !wikiSession.isAuthenticated() && wikiSession.isAnonymous()
         && author != null )
    {
        user  = findParam( pageContext, "author" );
    }

    //
    //  WYSIWYG editor sends us its greetings
    //
    String htmlText = findParam( pageContext, "htmlPageText" );
    if( htmlText != null && cancel == null )
    {
        text = new HtmlStringToWikiTranslator().translate(htmlText,wikiContext);
    }

    WikiPage wikipage = wikiContext.getPage();
    WikiPage latestversion = wiki.getPage( pagereq );

    if( latestversion == null )
    {
        latestversion = wikiContext.getPage();
    }

    //
    //  Set the response type before we branch.
    //

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    response.setHeader( "Cache-control", "max-age=0" );
    response.setDateHeader( "Expires", new Date().getTime() );
    response.setDateHeader( "Last-Modified", new Date().getTime() );

    //log.debug("Request character encoding="+request.getCharacterEncoding());
    //log.debug("Request content type+"+request.getContentType());
    log.debug("preview="+preview+", ok="+ok);

    if( ok != null || captcha != null )
    {
        log.info("Saving page "+pagereq+". User="+user+", host="+request.getRemoteAddr() );

        WikiPage modifiedPage = (WikiPage)wikiContext.getPage().clone();

        //  FIXME: I am not entirely sure if the JSP page is the
        //  best place to check for concurrent changes.  It certainly
        //  is the best place to show errors, though.

        long pagedate   = Long.parseLong(edittime);

        Date change = latestversion.getLastModified();

        if( change != null && change.getTime() != pagedate )
        {
            //
            // Someone changed the page while we were editing it!
            //

            log.info("Page changed, warning user.");

            session.setAttribute( EditorManager.REQ_EDITEDTEXT, EditorManager.getEditedText(pageContext) );
            response.sendRedirect( wiki.getURL(WikiContext.CONFLICT, pagereq, null, false) );
            return;
        }

        //
        //  We expire ALL locks at this moment, simply because someone has
        //  already broken it.
        //
        PageLock lock = wiki.getPageManager().getCurrentLock( wikipage );
        wiki.getPageManager().unlockPage( lock );
        session.removeAttribute( "lock-"+pagereq );

        //
        //  Set author information and other metadata
        //

        modifiedPage.setAuthor( user );

        if( changenote == null ) changenote = (String) session.getAttribute("changenote");

        session.removeAttribute("changenote");

        if( changenote != null && changenote.length() > 0 )
        {
            modifiedPage.setAttribute( WikiPage.CHANGENOTE, changenote );
        }
        else
        {
            modifiedPage.removeAttribute( WikiPage.CHANGENOTE );
        }

        //
        //  Figure out the actual page text
        //

        if( text == null )
        {
            throw new ServletException( "No parameter text set!" );
        }

        //
        //  If this is an append, then we just append it to the page.
        //  If it is a full edit, then we will replace the previous contents.
        //

        try
        {
            wikiContext.setPage( modifiedPage );

            if( captcha != null )
            {
                wikiContext.setVariable( "captcha", Boolean.TRUE );
                session.removeAttribute( "captcha" );
            }

            if( append != null )
            {
                StringBuffer pageText = new StringBuffer(wiki.getText( pagereq ));

                pageText.append( text );

                wiki.saveText( wikiContext, pageText.toString() );
            }
            else
            {
                wiki.saveText( wikiContext, text );
            }
        }
        catch( DecisionRequiredException ex )
        {
        	String redirect = wikiContext.getURL(WikiContext.VIEW,"ApprovalRequiredForPageChanges");
            response.sendRedirect( redirect );
            return;
        }
        catch( RedirectException ex )
        {
            // FIXME: Cut-n-paste code.
            //wikiContext.getWikiSession().addMessage( ex.getMessage() ); // FIXME: should work, but doesn't
            session.setAttribute( "message", ex.getMessage() );
            session.setAttribute(EditorManager.REQ_EDITEDTEXT,
                                 EditorManager.getEditedText(pageContext));
            session.setAttribute("author",user);
            session.setAttribute("link",link != null ? link : "" );
            if( htmlText != null ) session.setAttribute( EditorManager.REQ_EDITEDTEXT, text );

            session.setAttribute("changenote", changenote != null ? changenote : "" );
            session.setAttribute("edittime", edittime);
            session.setAttribute("addr", request.getRemoteAddr());
            response.sendRedirect( ex.getRedirect() );
            return;
        }

        response.sendRedirect(wiki.getViewURL(pagereq));
        return;
    }
    else if( preview != null )
    {
        log.debug("Previewing "+pagereq);
        session.setAttribute(EditorManager.REQ_EDITEDTEXT,
                             EditorManager.getEditedText(pageContext));
        session.setAttribute("author",user);
        session.setAttribute("link",link != null ? link : "" );

        if( htmlText != null ) session.setAttribute( EditorManager.REQ_EDITEDTEXT, text );

        session.setAttribute("changenote", changenote != null ? changenote : "" );
        response.sendRedirect( wiki.getURL(WikiContext.PREVIEW,pagereq,null,false) );
        return;
    }
    else if( cancel != null )
    {
        log.debug("Cancelled editing "+pagereq);
        PageLock lock = (PageLock) session.getAttribute( "lock-"+pagereq );

        if( lock != null )
        {
            wiki.getPageManager().unlockPage( lock );
            session.removeAttribute( "lock-"+pagereq );
        }
        response.sendRedirect( wiki.getViewURL(pagereq) );
        return;
    }

    session.removeAttribute( EditorManager.REQ_EDITEDTEXT );

    log.info("Editing page "+pagereq+". User="+user+", host="+request.getRemoteAddr() );

    //
    //  Determine and store the date the latest version was changed.  Since
    //  the newest version is the one that is changed, we need to track
    //  that instead of the edited version.
    //
    long lastchange = 0;

    Date d = latestversion.getLastModified();
    if( d != null ) lastchange = d.getTime();

    pageContext.setAttribute( "lastchange",
                              Long.toString( lastchange ),
                              PageContext.REQUEST_SCOPE );

    //
    //  Attempt to lock the page.
    //
    PageLock lock = wiki.getPageManager().lockPage( wikipage,
                                                    user );

    if( lock != null )
    {
        session.setAttribute( "lock-"+pagereq, lock );
    }

    String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                            wikiContext.getTemplate(),
                                                            "EditTemplate.jsp" );

%><wiki:Include page="<%=contentPage%>" />