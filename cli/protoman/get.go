/*
Copyright 2018 Spotify AB. All rights reserved.

The contents of this file are licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package protoman

import (
	"os"
	"path/filepath"
	"strings"

	"github.com/pkg/errors"
)

// Get package from protoman
func Get(packageName, path string) error {
	path = filepath.Join(path, strings.Replace(packageName, ".", "/", -1))
	if err := os.MkdirAll(path, 0755); err != nil {
		return errors.Wrap(err, "failed to create package path")
	}
	// TODO: add gRPC request to actually fetch the dependencies locally
	return addThirdPartyPackage(path)
}