package org.eclipse.ui.internal.genericeditor;


import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.codemining.ICodeMiningProvider;
import org.eclipse.jface.text.codemining.LineHeaderCodeMining;


public class ProblemMarkerCodeMining extends LineHeaderCodeMining
{
  public ProblemMarkerCodeMining(IDocument document, int lineNum, List<IMarker> markers, ICodeMiningProvider provider) throws BadLocationException
  {
    super(lineNum, document, provider);

    final String lineDelimiter = document.getLineDelimiter(lineNum);

    setLabel(createLabel(markers, lineDelimiter));
  }

  private String createLabel(List<IMarker> markers, String lineDelimiter)
  {
    //    final String label = markers.stream().map(this::convertMarkerToLabel).filter(Optional::isPresent).map(Optional::get).collect(Collectors.joining(lineDelimiter));
    final List<String> labels = markers.stream().map(this::convertMarkerToLabel).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    final String label = labels.get(new Random().nextInt(labels.size()));
    return label;
  }

  private Optional<String> convertMarkerToLabel(IMarker m)
  {
    try
    {
      Object message = m.getAttribute(IMarker.MESSAGE);
      if (message != null)
      {
        StringBuilder result = new StringBuilder();

        prependSeveritySymbol(m, result);

        result.append(message);

        return Optional.of(result.toString());
      }
    }
    catch (CoreException e)
    {
      // do nothing
    }
    return Optional.<String> empty();
  }

  private void prependSeveritySymbol(IMarker m, StringBuilder result) throws CoreException
  {
    Object severity = m.getAttribute(IMarker.SEVERITY);
    if (severity instanceof Integer)
    {
      switch ((Integer)severity)
      {
        case IMarker.SEVERITY_ERROR:
          result.append("⛔ "); //$NON-NLS-1$
          break;
        case IMarker.SEVERITY_WARNING:
          result.append("⚠️ "); //$NON-NLS-1$
          break;
        case IMarker.SEVERITY_INFO:
          result.append("ℹ️ "); //$NON-NLS-1$
          break;
        default:
          break;
      }
    }
  }
}