/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) 2018 EPAM Systems
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package com.epam.catgenome.manager.project;

import com.epam.catgenome.entity.project.Project;
import com.epam.catgenome.exception.FeatureIndexException;
import com.epam.catgenome.security.acl.aspect.AclFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class ProjectSecurityService {

    @Autowired
    private ProjectManager projectManager;

    @AclFilter
    @PreAuthorize("hasRole('USER')")
    public List<Project> loadTopLevelProjects() {
        return projectManager.loadTopLevelProjects();
    }

    public List<Project> loadProjectTree(Long parentId, String referenceName) {
        return projectManager.loadProjectTree(parentId, referenceName);
    }

    public Project load(Long projectId) {
        return projectManager.load(projectId);
    }

    public Project load(String projectName) {
        return projectManager.load(projectName);
    }

    public Project create(Project convertFrom, Long parentId) {
        return projectManager.create(convertFrom, parentId);
    }

    public void moveProjectToParent(Long projectId, Long parentId) {
        projectManager.moveProjectToParent(projectId, parentId);
    }

    public Project addProjectItem(Long projectId, Long biologicalItemId) {
        return projectManager.addProjectItem(projectId, biologicalItemId);
    }

    public Project removeProjectItem(Long projectId, Long biologicalItemId) throws FeatureIndexException {
        return projectManager.removeProjectItem(projectId, biologicalItemId);
    }

    public void hideProjectItem(Long projectId, Long biologicalItemId) {
        projectManager.hideProjectItem(projectId, biologicalItemId);
    }

    public Project deleteProject(long projectId, Boolean force) throws IOException {
        return projectManager.deleteProject(projectId, force);
    }
}
