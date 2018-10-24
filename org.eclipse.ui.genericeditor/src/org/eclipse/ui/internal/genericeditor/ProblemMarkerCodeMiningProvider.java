package org.eclipse.ui.internal.genericeditor;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.codemining.AbstractCodeMiningProvider;
import org.eclipse.jface.text.codemining.ICodeMining;
import org.eclipse.jface.text.codemining.LineContentCodeMining;


public class ProblemMarkerCodeMiningProvider extends AbstractCodeMiningProvider
{
  private Optional<IResource> extractResource(ITextViewer viewer)
  {
    final IDocument document = viewer.getDocument();
    if (document == null)
    {
      return Optional.empty();
    }
    final IPath location = FileBuffers.getTextFileBufferManager().getTextFileBuffer(document).getLocation();

    final IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(location);
    if (resource == null)
    {
      return Optional.empty();
    }

    return Optional.of(resource);
  }

  @Override
  public CompletableFuture<List<? extends ICodeMining>> provideCodeMinings(ITextViewer viewer, IProgressMonitor monitor)
  {
    return CompletableFuture.<List<? extends ICodeMining>> supplyAsync(() -> {
      try
      {
        final IResource resource = extractResource(viewer).get();
        final Map<Optional<Integer>, List<IMarker>> perLine = groupMarkersByLine(resource);
        final List<LineContentCodeMining> codeMinings = createCodeMinings(viewer.getDocument(), perLine);

        return codeMinings;
      }
      catch (CoreException e)
      {
        return Collections.emptyList();
      }
    });
  }

  private List<LineContentCodeMining> createCodeMinings(IDocument document, Map<Optional<Integer>, List<IMarker>> perLine)
  {
    final Stream<Entry<Optional<Integer>, List<IMarker>>> validEntries = perLine.entrySet().stream().filter(entry -> entry.getKey().isPresent());

    final List<LineContentCodeMining> codeMinings = validEntries.map(entry -> {
      try
      {
        final int lineNum = entry.getKey().get();
        final LineContentCodeMining codeMining = new ProblemMarkerCodeMining(document, lineNum, entry.getValue(), ProblemMarkerCodeMiningProvider.this);
        return Optional.of(codeMining);
      }
      catch (BadLocationException e)
      {
        return Optional.<LineContentCodeMining> empty();
      }
    }).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    return codeMinings;
  }

  private Map<Optional<Integer>, List<IMarker>> groupMarkersByLine(IResource resource) throws CoreException
  {
    final IMarker[] allMarkers = resource.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
    if (allMarkers == null)
    {
      return Collections.emptyMap();
    }

    final Map<Optional<Integer>, List<IMarker>> perLine = Arrays.stream(allMarkers).collect(Collectors.groupingBy(m -> {
      try
      {
        final Object line = m.getAttribute(IMarker.LINE_NUMBER);
        if (line instanceof Integer)
        {
          return Optional.of((Integer)line);
        }
        return Optional.empty();
      }
      catch (CoreException e)
      {
        return Optional.empty();
      }
    }));

    return perLine;
  }
}
