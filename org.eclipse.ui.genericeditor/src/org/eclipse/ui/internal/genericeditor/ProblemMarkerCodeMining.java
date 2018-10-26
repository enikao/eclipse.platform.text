package org.eclipse.ui.internal.genericeditor;


import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.codemining.ICodeMiningProvider;
import org.eclipse.jface.text.codemining.LineHeaderCodeMining;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.internal.ide.IDEInternalWorkbenchImages;


public class ProblemMarkerCodeMining extends LineHeaderCodeMining
{
  public static final int SEVERITY_UNKNOWN = -1;

  private int severity = SEVERITY_UNKNOWN;

  public ProblemMarkerCodeMining(IDocument document, IMarker marker, ICodeMiningProvider provider) throws IllegalArgumentException, BadLocationException
  {
    super(extractLineNumber(marker), document, provider);

    setLabel(convertMarkerToLabel(marker));

    extractSeverity(marker);
  }

  @Override
  public Point draw(GC gc, StyledText textWidget, Color color, int x, int y)
  {
    int textX = x;
    ImageDescriptor imageDescriptor;
    switch (this.severity)
    {
      case IMarker.SEVERITY_INFO:
        imageDescriptor = IDEInternalWorkbenchImages.getImageDescriptor(IDEInternalWorkbenchImages.IMG_OBJS_INFO_PATH);
        break;
      case IMarker.SEVERITY_WARNING:
        imageDescriptor = IDEInternalWorkbenchImages.getImageDescriptor(IDEInternalWorkbenchImages.IMG_OBJS_WARNING_PATH);
        break;
      case IMarker.SEVERITY_ERROR:
        imageDescriptor = IDEInternalWorkbenchImages.getImageDescriptor(IDEInternalWorkbenchImages.IMG_OBJS_ERROR_PATH);
        break;
      default:
        imageDescriptor = null;
        break;
    }

    if (imageDescriptor != null)
    {
      Image image = null;
      final int zoom = textWidget.getMonitor().getZoom();
      textX = x + imageDescriptor.getImageData(zoom).width + 2;
      image = imageDescriptor.createImage();
      if (image != null)
      {
        try
        {
          gc.drawImage(image, x, y);
        }
        finally
        {
          image.dispose();
        }
      }
    }

    return super.draw(gc, textWidget, color, textX, y);
  }

  private static int extractLineNumber(IMarker marker) throws IllegalArgumentException
  {
    try
    {
      Object lineNum = marker.getAttribute(IMarker.LINE_NUMBER);
      if (lineNum instanceof Integer)
      {
        final int result = Math.max(0, (Integer)lineNum - 1);
        return result;
      }
    }
    catch (CoreException e)
    {
      // do nothing
    }

    throw new IllegalArgumentException("cannot retrieve line number from " + marker); //$NON-NLS-1$
  }

  private void extractSeverity(IMarker marker)
  {
    try
    {
      Object severityObject = marker.getAttribute(IMarker.SEVERITY);
      if (severityObject instanceof Integer)
      {
        this.severity = (Integer)severityObject;
      }
    }
    catch (CoreException e)
    {
      // do nothing
    }
  }

  private String convertMarkerToLabel(IMarker marker) throws IllegalArgumentException
  {
    try
    {
      Object message = marker.getAttribute(IMarker.MESSAGE);
      if (message != null)
      {
        return message.toString();
        //        StringBuilder result = new StringBuilder();
        //        prependSeveritySymbol(marker, result);
        //        result.append(message);
        //        return result.toString();
      }
    }
    catch (CoreException e)
    {
      // do nothing
    }
    throw new IllegalArgumentException("cannot retrieve message from " + marker); //$NON-NLS-1$
  }

  //  private void prependSeveritySymbol(IMarker m, StringBuilder result) throws CoreException
  //  {
  //    Object severity = m.getAttribute(IMarker.SEVERITY);
  //    if (severity instanceof Integer)
  //    {
  //      switch ((Integer)severity)
  //      {
  //        case IMarker.SEVERITY_ERROR:
  //          result.append("⛔ "); //$NON-NLS-1$
  //          break;
  //        case IMarker.SEVERITY_WARNING:
  //          result.append("⚠️ "); //$NON-NLS-1$
  //          break;
  //        case IMarker.SEVERITY_INFO:
  //          result.append("ℹ️ "); //$NON-NLS-1$
  //          break;
  //        default:
  //          break;
  //      }
  //    }
  //  }
}