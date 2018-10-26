package org.eclipse.ui.internal.genericeditor;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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
import org.eclipse.jface.text.codemining.AbstractCodeMining;
import org.eclipse.jface.text.codemining.AbstractCodeMiningProvider;
import org.eclipse.jface.text.codemining.ICodeMining;


public class ProblemMarkerCodeMiningProvider extends AbstractCodeMiningProvider
{
  @Override
  public CompletableFuture<List<? extends ICodeMining>> provideCodeMinings(ITextViewer viewer, final IProgressMonitor monitor)
  {
    return CompletableFuture.<List<? extends ICodeMining>> supplyAsync(() -> {
      final Optional<IResource> extractedResource = extractResource(viewer);
      if (!extractedResource.isPresent())
      {
        return Collections.emptyList();
      }
      final IResource resource = extractedResource.get();
      final List<IMarker> markers = getMarkers(resource);
      final List<AbstractCodeMining> codeMinings = createCodeMinings(viewer.getDocument(), markers, monitor);

      return codeMinings;
    });
  }

  private static Optional<IResource> extractResource(ITextViewer viewer)
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

  private static List<IMarker> getMarkers(IResource resource)
  {
    try
    {
      IMarker[] allMarkers = resource.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
      if (allMarkers != null)
      {
        final List<IMarker> result = Arrays.asList(allMarkers);
        return result;
      }
    }
    catch (CoreException e)
    {
      // do nothing
    }
    return Collections.emptyList();
  }

  private List<AbstractCodeMining> createCodeMinings(IDocument document, List<IMarker> markers, IProgressMonitor monitor)
  {
    final List<AbstractCodeMining> result = markers.stream().filter(m -> !monitor.isCanceled()).filter(Objects::nonNull).map(m -> {
      try
      {
        return Optional.of(new ProblemMarkerCodeMining(document, m, this));
      }
      catch (BadLocationException | IllegalArgumentException e)
      {
        return Optional.<AbstractCodeMining> empty();
      }
    }).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    return result;
  }
}
