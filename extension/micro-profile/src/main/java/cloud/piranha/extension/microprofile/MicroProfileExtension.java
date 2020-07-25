/*
 * Copyright (c) 2002-2020 Manorrock.com. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   1. Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 *   2. Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *   3. Neither the name of the copyright holder nor the names of its
 *      contributors may be used to endorse or promote products derived from
 *      this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package cloud.piranha.extension.microprofile;

import cloud.piranha.security.jakarta.JakartaSecurityExtension;
import cloud.piranha.webapp.api.WebApplicationExtension;
import cloud.piranha.webapp.api.WebApplicationExtensionContext;
import cloud.piranha.webapp.scinitializer.ServletContainerInitializerExtension;
import cloud.piranha.webapp.webxml.WebXmlExtension;

/**
 * The MicroProfile extension.
 *
 * <p>
 * This web application extension delivers the basics for a MicroProfile
 * compatible implementation.
 * </p>
 *
 * <ul>
 * <li>MicroProfile JWT Auth</li>
 * </ul>
 *
 * @author Thiago Henrique Hupner
 * @author Manfred Riem (mriem@manorrock.com)
 * @see cloud.piranha.webapp.api.WebApplicationExtension
 */
public class MicroProfileExtension implements WebApplicationExtension {

    /**
     * Extend the web application.
     *
     * @param context the context.
     */
    @Override
    public void extend(WebApplicationExtensionContext context) {
        context.add(new WebXmlExtension());
        context.add(new JakartaSecurityExtension());
        context.add(new ServletContainerInitializerExtension());
    }
}
